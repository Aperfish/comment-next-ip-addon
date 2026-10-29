package com.xhhao.comment.widget.iplocation;
import java.util.*;
final class SplitIpNames {
    private static final Map<String,String> NAMES = new HashMap<>();
    static {
        add("美国", "Alabama=阿拉巴马州|Alaska=阿拉斯加州|Arizona=亚利桑那州|Arkansas=阿肯色州|California=加利福尼亚州|Colorado=科罗拉多州|Connecticut=康涅狄格州|Delaware=特拉华州|Florida=佛罗里达州|Georgia=佐治亚州|Hawaii=夏威夷州|Idaho=爱达荷州|Illinois=伊利诺伊州|Indiana=印第安纳州|Iowa=艾奥瓦州|Kansas=堪萨斯州|Kentucky=肯塔基州|Louisiana=路易斯安那州|Maine=缅因州|Maryland=马里兰州|Massachusetts=马萨诸塞州|Michigan=密歇根州|Minnesota=明尼苏达州|Mississippi=密西西比州|Missouri=密苏里州|Montana=蒙大拿州|Nebraska=内布拉斯加州|Nevada=内华达州|New Hampshire=新罕布什尔州|New Jersey=新泽西州|New Mexico=新墨西哥州|New York=纽约|North Carolina=北卡罗来纳州|North Dakota=北达科他州|Ohio=俄亥俄州|Oklahoma=俄克拉何马州|Oregon=俄勒冈州|Pennsylvania=宾夕法尼亚州|Rhode Island=罗得岛州|South Carolina=南卡罗来纳州|South Dakota=南达科他州|Tennessee=田纳西州|Texas=得克萨斯州|Utah=犹他州|Vermont=佛蒙特州|Virginia=弗吉尼亚州|Washington=华盛顿州|West Virginia=西弗吉尼亚州|Wisconsin=威斯康星州|Wyoming=怀俄明州|Washington, D.C.=华盛顿哥伦比亚特区|District of Columbia=哥伦比亚特区|Washington D.C.=华盛顿哥伦比亚特区|College Park=大学公园市|Columbus=哥伦布|New York City=纽约市|Miami=迈阿密|Los Angeles=洛杉矶|San Francisco=旧金山|San Jose=圣何塞|Seattle=西雅图|Chicago=芝加哥|Dallas=达拉斯|Houston=休斯敦|Boston=波士顿|Atlanta=亚特兰大|Las Vegas=拉斯维加斯|Ashburn=阿什本|Mountain View=山景城|San Diego=圣迭戈|Phoenix=菲尼克斯|Denver=丹佛|Philadelphia=费城|Portland=波特兰|Buffalo=布法罗|Fremont=弗里蒙特|Santa Clara=圣克拉拉|Sacramento=萨克拉门托|Baltimore=巴尔的摩|Pittsburgh=匹兹堡|Kansas City=堪萨斯城|Salt Lake City=盐湖城");
        add("日本", "Tokyo=东京|Tokyo-to=东京都|Osaka=大阪|Kyoto=京都|Nagoya=名古屋|Fukuoka=福冈|Yokohama=横滨|Saitama=埼玉|Chiba=千叶|Hokkaido=北海道|Sapporo=札幌");
        add("英国", "England=英格兰|London=伦敦|Manchester=曼彻斯特|Scotland=苏格兰|Edinburgh=爱丁堡|Wales=威尔士");
        add("台湾", "Taipei=台北|Taipei City=台北|New Taipei=新北|New Taipei City=新北|Taichung=台中|Kaohsiung=高雄|Tainan=台南|Taoyuan=桃园|Hsinchu=新竹");
        add("香港", "Hong Kong=香港|Kowloon=九龙|Tsuen Wan=荃湾|Central=中环|New Territories=新界");
        add("新加坡", "Singapore=新加坡");
        add("德国", "Frankfurt am Main=美因河畔法兰克福|Frankfurt=法兰克福|Hesse=黑森州|Hessen=黑森州|Berlin=柏林|Munich=慕尼黑|Bavaria=巴伐利亚州");
        add("澳大利亚", "Queensland=昆士兰州|Brisbane=布里斯班|Sydney=悉尼|New South Wales=新南威尔士州|Melbourne=墨尔本|Victoria=维多利亚州");
        add("加拿大", "Ontario=安大略省|Toronto=多伦多|Quebec=魁北克省|Montreal=蒙特利尔|British Columbia=不列颠哥伦比亚省|Vancouver=温哥华");
    }
    private static void add(String country,String data) { for(String item:data.split("\\|")){String[] p=item.split("=",2);NAMES.put(country+"|"+p[0].toLowerCase(Locale.ROOT),p[1]);} }
    static String local(String country,String value,boolean region) {
        value=SplitIpEngine.clean(value);
        if(value.matches(".*[\\p{IsHan}].*"))return value;
        if(country.equals("美国") && value.equalsIgnoreCase("New York"))return region?"纽约州":"纽约市";
        if(country.equals("美国") && value.equalsIgnoreCase("Washington") && !region)return "华盛顿";
        return NAMES.getOrDefault(country+"|"+value.toLowerCase(Locale.ROOT),value);
    }
}
