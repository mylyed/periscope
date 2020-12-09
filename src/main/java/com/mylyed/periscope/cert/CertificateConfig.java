package com.mylyed.periscope.cert;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * 密钥配置
 *
 * @author lilei
 * @create 2020-12-09
 **/
public class CertificateConfig {
    X509Certificate rootX509Certificate;
    PrivateKey rootPrivateKey;
    //生产一对随机公私钥
    KeyPair clientKeyPair;

    public CertificateConfig(X509Certificate rootX509Certificate, PrivateKey rootPrivateKey) {
        this.rootX509Certificate = rootX509Certificate;
        this.rootPrivateKey = rootPrivateKey;
        try {
            clientKeyPair = CertUtil.genKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public X509Certificate getRootX509Certificate() {
        return rootX509Certificate;
    }

    public PrivateKey getRootPrivateKey() {
        return rootPrivateKey;
    }

    public KeyPair getClientKeyPair() {
        return clientKeyPair;
    }
}
