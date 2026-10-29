public class AdapterLinkTest {
 public static void main(String[] args)throws Exception {
  Class<?> adapter=Class.forName("io.github.commentipaddon.HostIpService");
  Object instance=adapter.getConstructor(java.util.function.Function.class).newInstance((java.util.function.Function<String,Object>)(s->null));
  if(!Class.forName("com.xhhao.comment.widget.iplocation.CommentNextIpLocationService").isInstance(instance))throw new AssertionError();
  System.out.println("PASS adapter links and constructs against actual original 1.0.13 service (Reactor compile-signature fixture; not a Halo runtime test).");
 }
}
