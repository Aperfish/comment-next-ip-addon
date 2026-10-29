import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.pf4j.*;
import run.halo.app.plugin.ReactiveSettingFetcher;
public class InjectionRegressionTest {
    public static void main(String[] args)throws Exception {
        AtomicInteger calls=new AtomicInteger();
        PluginManager manager=id->{if(!id.equals("PluginCommentNext"))throw new AssertionError(id);calls.incrementAndGet();return null;};
        PluginWrapper wrapper=new PluginWrapper(){public PluginManager getPluginManager(){return manager;}};
        ReactiveSettingFetcher settings=(ReactiveSettingFetcher)Proxy.newProxyInstance(ReactiveSettingFetcher.class.getClassLoader(),new Class[]{ReactiveSettingFetcher.class},(p,m,a)->{throw new AssertionError("Settings not needed during constructor");});
        Class<?> lifecycle=Class.forName("io.github.commentipaddon.AddonLifecycle");
        var constructors=lifecycle.getConstructors();
        if(constructors.length!=1 || constructors[0].getParameterTypes()[0]!=PluginWrapper.class)throw new AssertionError("Wrong injection type");
        Object bean=constructors[0].newInstance(wrapper,settings);
        try {lifecycle.getMethod("afterPropertiesSet").invoke(bean);throw new AssertionError("Missing host accepted");}
        catch(InvocationTargetException e){if(!(e.getCause() instanceof IllegalStateException)||!e.getCause().getMessage().contains("original PluginCommentNext"))throw e;}
        if(calls.get()!=1)throw new AssertionError("Wrapper's manager not used");
        lifecycle.getMethod("destroy").invoke(bean);
        System.out.println("PASS regression: constructor requires registered PluginWrapper, gets manager from wrapper, detects missing host, safely destroys after attachment failure. Uses API-signature fixtures, not live Halo.");
    }
}
