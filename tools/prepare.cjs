const fs=require('fs'),path=require('path');
function write(p,s){fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s);}
const stub='work/addon/stubs/';
const defs={
'org/springframework/beans/factory/InitializingBean.java':'package org.springframework.beans.factory; public interface InitializingBean {void afterPropertiesSet() throws Exception;}',
'org/springframework/beans/factory/DisposableBean.java':'package org.springframework.beans.factory; public interface DisposableBean {void destroy() throws Exception;}',
'org/springframework/context/ApplicationContext.java':'package org.springframework.context; public interface ApplicationContext {<T>T getBean(Class<T> type);}',
'org/pf4j/Plugin.java':'package org.pf4j; public class Plugin {}',
'org/pf4j/PluginWrapper.java':'package org.pf4j; public class PluginWrapper {public PluginManager getPluginManager(){throw new UnsupportedOperationException();} public Plugin getPlugin(){throw new UnsupportedOperationException();} public ClassLoader getPluginClassLoader(){throw new UnsupportedOperationException();}}',
'org/pf4j/PluginManager.java':'package org.pf4j; public interface PluginManager {PluginWrapper getPlugin(String id);}',
'run/halo/app/plugin/SpringPluginManager.java':'package run.halo.app.plugin; import org.pf4j.PluginWrapper; public interface SpringPluginManager {PluginWrapper getPlugin(String id);}',
'run/halo/app/plugin/SpringPlugin.java':'package run.halo.app.plugin; import org.springframework.context.ApplicationContext; public interface SpringPlugin {ApplicationContext getApplicationContext();}'
};
for(const [p,s]of Object.entries(defs))write(stub+p,s);
let mono=fs.readFileSync(stub+'reactor/core/publisher/Mono.java','utf8');if(!mono.includes('Mono<T> just('))mono=mono.replace('public abstract class Mono<T> {','public abstract class Mono<T> { public static <T> Mono<T> just(T v){throw new UnsupportedOperationException();}');write(stub+'reactor/core/publisher/Mono.java',mono);
function files(root){return fs.readdirSync(root,{withFileTypes:true}).flatMap(x=>x.isDirectory()?files(root+'/'+x.name):[root+'/'+x.name]);}
write('work/addon/stubs.args',files(stub).filter(x=>x.endsWith('.java')).join('\n'));
write('work/addon/src.args',files('work/addon/src').filter(x=>x.endsWith('.java')).join('\n'));
write('work/addon/classes/plugin.yaml',fs.readFileSync('src/main/resources/plugin.yaml','utf8'));
write('work/addon/classes/extensions/settings.yaml',fs.readFileSync('src/main/resources/extensions/settings.yaml','utf8'));
write('work/addon/classes/META-INF/plugin-components.idx','io.github.commentipaddon.AddonLifecycle\n');
write('work/addon/manifest.mf','Manifest-Version: 1.0\nPlugin-Main-Class: run.halo.app.plugin.BasePlugin\nImplementation-Title: comment-next-ip-addon\nImplementation-Version: 1.0.13-4\n\n');





fs.copyFileSync('assets/logo.png','work/addon/classes/logo.png');

