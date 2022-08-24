package com.rpccenter.remote.codec.compress;

import com.common.extension.SPI;

@SPI
public interface Compress {
    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}
