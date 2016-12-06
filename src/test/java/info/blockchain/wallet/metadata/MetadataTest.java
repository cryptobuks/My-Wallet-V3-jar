package info.blockchain.wallet.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.api.MetadataEndpoints;
import info.blockchain.bip44.Wallet;
import info.blockchain.bip44.WalletFactory;

import org.junit.Before;
import org.junit.Test;

import io.jsonwebtoken.lang.Assert;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class MetadataTest {

    boolean isEncrypted = false;

    MetadataEndpoints httpClient;
    MockInterceptor mockInterceptor;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() throws Exception {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        mockInterceptor = new MockInterceptor();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)//Mock responses
//                .addInterceptor(loggingInterceptor)//Extensive logging
                .build();

        httpClient = RestClient.getClient(okHttpClient);
    }

    private Wallet getWallet() throws Exception {

        return new WalletFactory().restoreWallet("15e23aa73d25994f1921a1256f93f72c",
                "",
                1);
    }

    @Test
    public void testAddressDerivation() throws Exception {

        String address = "12sC9tqHzAhdoukhCbTnyx2MjYXNXBGHnF";

        mockInterceptor.setResponse_404();//New metadata response

        Metadata metadata = new MetadataBuilder(httpClient)
                .setPurpose(MetadataBuilder.PURPOSE_BASIC)
                .setRootNode(getWallet().getMasterKey())
                .setType(2)
                .setEncrypted(true)
                .build();

        Assert.isTrue(metadata.getAddress().equals(address));
    }

    @Test
    public void testMetadata() throws Exception{

        mockInterceptor.setResponse_404();//New metadata response

        Metadata metadata = new MetadataBuilder(httpClient)
                .setPurpose(MetadataBuilder.PURPOSE_BASIC)
                .setRootNode(getWallet().getMasterKey())
                .setType(Metadata.PAYLOAD_TYPE_RESERVED)
                .setEncrypted(isEncrypted)
                .build();

        String msg = "Rage rage";
        mockInterceptor.setResponse_PUT_rage();
        metadata.putMetadata(mapper.writeValueAsString(msg));

        mockInterceptor.setResponse_GET_rage();
        String result1 = metadata.getMetadata();

        Assert.isTrue(msg.equals(result1));

        mockInterceptor.setResponse_PUT_more_rage();
        msg = "Rage rage some more";
        metadata.putMetadata(mapper.writeValueAsString(msg));

        mockInterceptor.setResponse_GET_more_rage();
        String result2 = metadata.getMetadata();
        Assert.isTrue(msg.equals(result2));

        mockInterceptor.setResponse_DELETE_ok();
        metadata.deleteMetadata(msg);

        mockInterceptor.setResponse_404();
        try {
            metadata.getMetadata();
            Assert.isTrue(false);
        }catch (Exception e){
            Assert.isTrue(true);
        }
    }
}