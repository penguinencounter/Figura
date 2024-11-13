package org.figuramc.figura.ducks;

import java.io.IOException;

public interface NativeImageExtension {
    public byte[] figura$asByteArray() throws IOException;
}
