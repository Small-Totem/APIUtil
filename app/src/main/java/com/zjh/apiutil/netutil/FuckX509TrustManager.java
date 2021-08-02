package com.zjh.apiutil.netutil;
import android.annotation.SuppressLint;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class FuckX509TrustManager implements X509TrustManager{

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
