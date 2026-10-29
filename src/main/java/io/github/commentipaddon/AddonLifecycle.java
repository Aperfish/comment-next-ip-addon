package io.github.commentipaddon;
import java.lang.reflect.*;
import java.util.function.Function;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.*;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import com.xhhao.comment.widget.iplocation.AddonResolver;

@Component
public final class AddonLifecycle implements InitializingBean,DisposableBean {
    private final PluginManager manager;
    private final ReactiveSettingFetcher settings;
    private Object mapper,original,installed;
    private Field field;
    public AddonLifecycle(PluginWrapper pluginWrapper,ReactiveSettingFetcher settings){this.manager=pluginWrapper.getPluginManager();this.settings=settings;}
    public synchronized void afterPropertiesSet() throws Exception {
        var dependency=manager.getPlugin("PluginCommentNext");
        if(dependency==null)throw new IllegalStateException("Install and start original PluginCommentNext 1.0.13 first");
        var context=((SpringPlugin)dependency.getPlugin()).getApplicationContext();
        Class<?> mapperClass=Class.forName("com.xhhao.comment.widget.comment.CommentNextCommentMapper",false,dependency.getPluginClassLoader());
        mapper=context.getBean(mapperClass);
        field=mapperClass.getDeclaredField("ipLocationService");field.setAccessible(true);original=field.get(mapper);
        Method resolve=original.getClass().getMethod("resolve",String.class);
        Field getterField=original.getClass().getDeclaredField("settingConfigGetter");getterField.setAccessible(true);
        Object getter=getterField.get(original);
        Method getConfig=getterField.getType().getMethod("getIpLocationConfig");
        Function<String,Mono<String>> fallback=ip->{try{return (Mono<String>)resolve.invoke(original,ip);}catch(Exception e){return Mono.just("");}};
        AddonResolver resolver=new AddonResolver(settings, fallback, ()->{
            try{return (Mono<?>)getConfig.invoke(getter);}catch(Exception e){return Mono.just(new Object());}
        });
        installed=new HostIpService(resolver::resolve);
        field.set(mapper,installed);
        if(field.get(mapper)!=installed)throw new IllegalStateException("Failed to attach IP addon");
        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO,"Comment IP addon 1.0.13-3 attached to original 1.0.13");
    }
    public synchronized void destroy() throws Exception {
        if(field!=null && mapper!=null && installed!=null && field.get(mapper)==installed)field.set(mapper,original);
        installed=null;mapper=null;original=null;field=null;
    }
}


