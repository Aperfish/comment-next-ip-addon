package io.github.commentipaddon;
import com.xhhao.comment.widget.iplocation.CommentNextIpLocationService;
import java.util.function.Function;
import reactor.core.publisher.Mono;
/** Adapter only; upstream classes are compile-only and never repackaged. */
public final class HostIpService extends CommentNextIpLocationService {
    private final Function<String,Mono<String>> resolver;
    public HostIpService(Function<String,Mono<String>> resolver) { super(null,null,null,null);this.resolver=resolver; }
    @Override public Mono<String> resolve(String ip) {
        try { return resolver.apply(ip).onErrorReturn(""); }
        catch(RuntimeException | LinkageError error){return Mono.just("");}
    }
}
