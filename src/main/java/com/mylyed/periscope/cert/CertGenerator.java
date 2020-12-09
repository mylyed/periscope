package com.mylyed.periscope.cert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lilei
 * @create 2020-12-09
 **/
public class CertGenerator {
    protected static final Logger log = LoggerFactory.getLogger(CertGenerator.class);

    private static final Map<String, X509Certificate> certCache = new WeakHashMap<>();
    public static CertificateConfig certificateConfig;

    static {
        try {
            ClassLoader classLoader = CertGenerator.class.getClassLoader();
            X509Certificate caCert = CertUtil.loadCert(classLoader.getResourceAsStream("ca.crt"));
            PrivateKey caPriKey = CertUtil.loadPriKey(Objects.requireNonNull(classLoader.getResourceAsStream("ca_private.der")));
            certificateConfig = new CertificateConfig(caCert, caPriKey);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }
    }

    static Lock lock = new ReentrantLock();

    public static X509Certificate getCert(String host)
            throws Exception {
        if (host != null) {
            if (certCache.containsKey(host)) {
                return certCache.get(host);
            } else {
                lock.lock();
                try {
                    if (!certCache.containsKey(host)) {
                        log.debug("创建证书：{}", host);
                        String subject = CertUtil.getSubject(certificateConfig.getRootX509Certificate());
                        Date notBefore = certificateConfig.getRootX509Certificate().getNotBefore();
                        Date notAfter = certificateConfig.getRootX509Certificate().getNotAfter();
                        KeyPair keyPair = certificateConfig.getClientKeyPair();
                        PublicKey serverPubKey = keyPair.getPublic();
                        X509Certificate cert = CertUtil.genCert(subject, certificateConfig.getRootPrivateKey(),
                                notBefore, notAfter,
                                serverPubKey, host);
                        certCache.put(host, cert);
                        return cert;
                    } else {
                        return getCert(host);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } else {
            throw new RuntimeException("host is null!");
        }
    }
}
