package com.mylyed.periscope.web;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * 网站图标
 *
 * @author lilei
 * @create 2020-11-28
 **/
public class Favicon {


    private static byte[] favicon = new byte[0];

    static {
        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(Favicon
                .class.getClassLoader().getResourceAsStream("favicon.png")))) {
            favicon = new byte[stream.available()];
            stream.read(favicon);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static byte[] favicon() {
        return favicon;
    }


}
