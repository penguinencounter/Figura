package org.figuramc.figura.lua.transfer;

/**
 * Make a copy of this type when transferred between avatars.
 */
public interface CopyOnTransfer<T> {
    T copy();
}
