package io.github.commentipaddon.internal.widget.iplocation;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
public class DistrictTest {
    static int n;
    static void eq(Object expected,Object actual){n++;if(!Objects.equals(expected,actual))throw new AssertionError(expected+" != "+actual);}
    static String baidu(String p,String c,String d){return "{\"status\":0,\"content\":{\"address_detail\":{\"province\":\""+p+"\",\"city\":\""+c+"\",\"district\":\""+d+"\"}}}";}
    public static void main(String[] args)throws Exception {
        String json=baidu("广东省","深圳市","南山区");
        eq("广东·深圳·南山区",SplitIpEngine.parse(json,true));
        eq("广东·深圳",SplitIpEngine.parse(json,false));
        eq("吉林·吉林·船营区",SplitIpEngine.parse(baidu("吉林省","吉林市","船营区"),true));
        eq("吉林·吉林",SplitIpEngine.parse(baidu("吉林省","吉林市","船营区"),false));
        for(String city:List.of("北京","上海","天津","重庆"))for(boolean on:List.of(true,false))eq(on ? city+"·测试区" : city,SplitIpEngine.parse(baidu(city+"市",city+"市","测试区"),on));
        for(String district:List.of("","0","unknown","440305","市辖区"))eq("广东·深圳",SplitIpEngine.parse(baidu("广东省","深圳市",district),true));
        eq("广东·深圳",SplitIpEngine.parse(baidu("广东省","深圳市","深圳市"),true));
        eq("四川·成都·新津区",SplitIpEngine.parse("{\"data\":{\"country\":\"中国\",\"prov\":\"四川省\",\"city\":\"成都市\",\"county\":\"新津区\"}}",true));
        eq("美国·俄亥俄州·哥伦布",SplitIpEngine.parse("{\"countryCode\":\"US\",\"regionName\":\"Ohio\",\"city\":\"Columbus\",\"district\":\"foo\"}",true));
        eq("台湾·台北",SplitIpEngine.parse("{\"countryCode\":\"TW\",\"city\":\"台北市\",\"district\":\"中山区\"}",true));
        eq("",SplitIpEngine.parse("{\"status\":1}",true));
        String preserved=CommentNextIpLocationResponseParser.location(new ObjectMapper().readTree(json));
        eq(true,preserved.contains("南山区"));
        eq("广东·深圳·南山区",SplitIpEngine.parse(preserved,true));
        var on=new SplitIpEngine.Options(true,"https://domestic/{ip}","https://foreign/{ip}",1440,500,true,true);
        var off=new SplitIpEngine.Options(true,"https://domestic/{ip}","https://foreign/{ip}",1440,500,true,false);
        eq(false,on.equals(off));
        var engine=new SplitIpEngine(ip->"中国|广东省|深圳市|0|CN");AtomicInteger requests=new AtomicInteger();
        for(var option:List.of(off,on,off,on)) {
            String expected=option.domesticDistrictEnabled()?"广东·深圳·南山区":"广东·深圳";
            eq(expected,engine.resolve("223.5.5.5",option,(a,b,c)->{requests.incrementAndGet();return SplitIpEngine.parse(json,option.domesticDistrictEnabled());}));
        }
        eq(2,requests.get());
        eq("广东·深圳",new SplitIpEngine(ip->"中国|广东省|深圳市|0|CN").resolve("223.5.5.5",on,(a,b,c)->""));
        System.out.println("PASS "+n+" district assertions against final addon JAR; includes preserved API fields, switch/cache isolation, municipalities, fallback and overseas regressions.");
    }
}
