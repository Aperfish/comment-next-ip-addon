package com.xhhao.comment.widget.iplocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.xhhao.comment.widget.network.CommentNextOutboundUriPolicy;
import run.halo.app.plugin.ReactiveSettingFetcher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.*;
import java.util.function.*;

public final class AddonResolver {
    private final ReactiveSettingFetcher settings;
    private final Function<String,Mono<String>> original;
    private final Supplier<Mono<?>> hostConfig;
    private final SplitIpEngine engine=new SplitIpEngine(new SplitIpEngine.Database());
    private final CommentNextIpLocationClient client=new CommentNextIpLocationClient(new CommentNextOutboundUriPolicy());
    public AddonResolver(ReactiveSettingFetcher settings,Function<String,Mono<String>> original,Supplier<Mono<?>> hostConfig){this.settings=settings;this.original=original;this.hostConfig=hostConfig;}
    public Mono<String> resolve(String ip) {
        return hostConfig.get().flatMap(host->{
            try {if(!(boolean)host.getClass().getMethod("isEnabled").invoke(host))return Mono.just("");}catch(Exception e){return Mono.just("");}
            return settings.getValues().defaultIfEmpty(Map.of()).flatMap(values->{
                JsonNode config=values.getOrDefault("ipLocation",JsonNodeFactory.instance.objectNode());
                if(!config.path("enabled").asBoolean(true))return original.apply(ip);
                var options=new SplitIpEngine.Options(true,config.path("domesticApiUrlTemplate").asText(""),config.path("foreignApiUrlTemplate").asText(""),config.path("cacheMinutes").asInt(1440),config.path("timeoutMillis").asInt(3000),config.path("offlineFallback").asBoolean(true),config.path("domesticDistrictEnabled").asBoolean(false));
                return Mono.fromCallable(()->SplitIpTranslator.translate(engine.resolve(ip,options,(a,b,c)->SplitIpEngine.parse(client.fetchLocation(a,b,c).block(c.plusMillis(250)),options.domesticDistrictEnabled())),
                    config.path("translationUrl").asText(""),config.path("translationModel").asText(""),config.path("translationApiKey").asText("")))
                    .subscribeOn(Schedulers.boundedElastic()).onErrorReturn("");
            });
        }).onErrorReturn("");
    }
}
