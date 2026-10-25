import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

public final class ApkSign {
    public static void main(String[] a) throws Exception {
        // args: in.apk out.apk keystore alias storepass minsdk
        File in = new File(a[0]), out = new File(a[1]);
        char[] pw = a[4].toCharArray();
        KeyStore ks = KeyStore.getInstance(new File(a[2]), pw);
        PrivateKey key = (PrivateKey) ks.getKey(a[3], pw);
        X509Certificate cert = (X509Certificate) ks.getCertificate(a[3]);
        List<X509Certificate> chain = Collections.singletonList(cert);

        ApkSigner.SignerConfig cfg = new ApkSigner.SignerConfig.Builder(a[3], key, chain).build();
        new ApkSigner.Builder(Collections.singletonList(cfg))
                .setInputApk(in)
                .setOutputApk(out)
                .setMinSdkVersion(Integer.parseInt(a[5]))
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign();

        ApkVerifier.Result r = new ApkVerifier.Builder(out).build().verify();
        System.out.println("verified=" + r.isVerified()
                + " v1=" + r.isVerifiedUsingV1Scheme()
                + " v2=" + r.isVerifiedUsingV2Scheme()
                + " v3=" + r.isVerifiedUsingV3Scheme()
                + " errors=" + r.getErrors());
        if (!r.isVerified()) System.exit(2);
    }
}
