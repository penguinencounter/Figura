package org.figuramc.figura.lua.transfer;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.lua.api.AvatarAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

public class ProtectedFunction extends VarArgFunction {
    private static abstract class ProtectionTransformer implements LuaTransformer {
        protected LuaTransformer inverse;

        void setInverse(LuaTransformer inverse) {
            this.inverse = inverse;
        }
    }

    private static class Nothing extends ProtectionTransformer {
    }

    private class Low extends ProtectionTransformer {
        protected final boolean isInward;
        private final LuaTypeManager targetTypes;

        public Low(boolean isInward) {
            this.targetTypes = isInward ? provider.luaRuntime.typeManager : consumer.luaRuntime.typeManager;
            this.isInward = isInward;
        }

        @Override
        public @NotNull LuaValue userdata(LuaUserdata usr) {
            return targetTypes.wrap(usr.m_instance);
        }

        @Override
        public @NotNull LuaTable table(LuaTable t) {
            return new LazySyncTable(t, this, inverse);
        }

        @Override
        public @NotNull LuaValue function(LuaFunction f) {
            if (isInward) {
                // i.e. consumer passes a function to provider
                // NOTE: switched 'consumer' and 'provider', because the person calling the function
                // is the 'owner' of this function, but we still want to enforce the callee's rules
                return new ProtectedFunction(f, consumer, provider);
            } else {
                // i.e. returns a callback; inherit pretty much everything
                return new ProtectedFunction(f, provider, consumer);
            }
        }
    }

    private class Default extends Low {
        protected final LuaTypeManager targetTypes;
        protected final boolean isProvider;

        public Default(boolean isInward, boolean isProvider) {
            super(isInward);
            this.targetTypes = isInward ? provider.luaRuntime.typeManager : consumer.luaRuntime.typeManager;
            this.isProvider = isProvider;
        }

        @Override
        public @NotNull LuaValue userdata(LuaUserdata usr) {
            Object inner = usr.m_instance;
            // perhaps the provider wants mutability on purpose, so don't override their choices here
            if (isProvider)
                if (inner instanceof CopyOnTransfer<?> copyable) inner = copyable.copy();
            return targetTypes.wrap(inner);
        }

        @Override
        public @NotNull LuaTable table(LuaTable t) {
            // same policy as above but forcing the redaction of metatables on both ends
            if (isProvider)
                return TransformerLuaTable.make(t, this).removeMeta();
            else
                return new LazySyncTable(t, this, inverse).removeMeta();
        }
    }

    private ProtectionTransformer getTransformer(FunctionProtectLevel level, boolean inward, boolean provider) {
        return switch (level) {
            case NOTHING -> new Nothing();
            case LOW -> new Low(inward);
            case DEFAULT -> new Default(inward, provider);
        };
    }

    public final LuaFunction around;
    public final @Nullable FunctionProtectLevel ownerOverride;
    public final Avatar provider;
    public final Avatar consumer;

    /**
     * Create a new protected function wrapper.
     *
     * @param around   Target function
     * @param provider Owner of function
     * @param consumer Consumer of function
     */
    public ProtectedFunction(LuaFunction around,
                             Avatar provider,
                             Avatar consumer) {
        if (around instanceof PartiallyProtectedFunction partial) {
            this.around = partial.around;
            this.ownerOverride = partial.providerLevel;
        } else {
            this.around = around;
            this.ownerOverride = null;
        }
        this.provider = provider;
        this.consumer = consumer;
    }

    @Override
    public String tojstring() {
        return String.format(
                "prot %s (%s → %s)",
                around.tojstring(),
                provider.entityName,
                consumer.entityName
        );
    }

    @Override
    public Varargs invoke(Varargs args) {
        FiguraLuaRuntime consumerRuntime = this.consumer.luaRuntime;
        AvatarAPI consumerOptions = consumerRuntime.avatar_meta;
        FunctionProtectLevel consumerLevel = consumerOptions.consuming;

        FiguraLuaRuntime providerRuntime = this.provider.luaRuntime;
        AvatarAPI providerOptions = providerRuntime.avatar_meta;
        FunctionProtectLevel providerLevel = ownerOverride != null ? ownerOverride : providerOptions.providing;

        // Create transformers
        ProtectionTransformer inTransform1 = getTransformer(consumerLevel, true, false);
        ProtectionTransformer inTransform2 = getTransformer(providerLevel, true, true);
        ProtectionTransformer outTransform2 = getTransformer(providerLevel, false, true);
        ProtectionTransformer outTransform1 = getTransformer(consumerLevel, false, false);

        inTransform1.setInverse(outTransform1);
        outTransform1.setInverse(inTransform1);
        inTransform2.setInverse(outTransform2);
        outTransform2.setInverse(inTransform2);

        // Transform inputs...
        LuaValue[] transformedArgs = new LuaValue[args.narg()];
        for (int i = 1; i <= args.narg(); i++)
            transformedArgs[i - 1] =
                    inTransform2.visit(inTransform1.visit(args.arg(i)));
        // Call the actual function...
        Varargs rets;
        try (FiguraLuaRuntime.AvatarContext ignored = FiguraLuaRuntime.context(provider)) {
            rets = around.invoke(transformedArgs);
        }
        // Transform outputs...
        LuaValue[] transformedRets = new LuaValue[rets.narg()];
        for (int i = 1; i <= rets.narg(); i++)
            transformedRets[i - 1] =
                    outTransform1.visit(outTransform2.visit(rets.arg(i)));
        // Done.
        return varargsOf(transformedRets);
    }
}
