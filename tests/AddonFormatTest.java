import java.util.*;
import java.lang.reflect.*;
public class AddonFormatTest {
    static int n;
    static void eq(String a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args)throws Exception {
        Class<?> engine=Class.forName("io.github.commentipaddon.internal.widget.iplocation.SplitIpEngine");
        Method domestic=engine.getDeclaredMethod("domestic",String.class,String.class,String.class);domestic.setAccessible(true);
        eq("吉林·吉林",domestic.invoke(null,"吉林省","吉林市","船营区"));
        eq("吉林·长春",domestic.invoke(null,"吉林省","长春市",""));
        eq("广东·深圳",domestic.invoke(null,"广东省","深圳市",""));
        eq("吉林",domestic.invoke(null,"吉林省","",""));
        eq("吉林",domestic.invoke(null,"","吉林市",""));
        for(String city:List.of("北京","上海","天津","重庆")) {
            eq(city+"·测试区",domestic.invoke(null,city+"市",city+"市","测试区"));
            eq(city,domestic.invoke(null,city+"市",city+"市",""));
        }
        eq("重庆·奉节县",domestic.invoke(null,"重庆市","重庆市","奉节县"));
        Method parse=engine.getMethod("parse",String.class);
        eq("吉林·吉林",parse.invoke(null,"{\"status\":0,\"content\":{\"address_detail\":{\"province\":\"吉林省\",\"city\":\"吉林市\"}}}"));
        eq("台湾·台北",parse.invoke(null,"{\"countryCode\":\"TW\",\"city\":\"台北市\"}"));
        eq("香港·九龙·九龙城",parse.invoke(null,"{\"countryCode\":\"HK\",\"country\":\"中国香港特别行政区\",\"regionName\":\"九龙\",\"city\":\"九龙城\"}"));
        eq("美国·俄亥俄州·哥伦布",parse.invoke(null,"{\"countryCode\":\"US\",\"regionName\":\"Ohio\",\"city\":\"Columbus\"}"));
        Method offline=engine.getMethod("offline",String[].class);
        eq("吉林·吉林",offline.invoke(null,(Object)new String[]{"中国","吉林省","吉林市","0","CN"}));
        System.out.println("PASS "+n+" assertions against final JAR: online/offline same-name cities, all four municipalities, missing fields, Taiwan, Hong Kong and unchanged foreign names.");
    }
}


