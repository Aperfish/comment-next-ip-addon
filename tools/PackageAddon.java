import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.nio.file.*;
import java.util.*;
public class PackageAddon {
 public static void main(String[]args)throws Exception {
  Path out=Path.of("work/addon/classes");List<Path> classes=new ArrayList<>();
  try(var f=Files.walk(Path.of("work/addon/compiled"))){classes.addAll(f.filter(p->p.toString().endsWith(".class")).toList());}
  for(String s:List.of("iplocation/CommentNextIpLocationClient","network/CommentNextOutboundUriPolicy"))classes.add(Path.of("work/v1013/original/com/xhhao/comment/widget/"+s+".class"));
  classes.add(Path.of("work/v1013/original/com/xhhao/comment/utils/JsonUtils.class"));
  Set<String> relocated=new HashSet<>();for(Path p:classes){String n=new ClassReader(Files.readAllBytes(p)).getClassName();if(n.startsWith("com/xhhao/"))relocated.add(n);}
  Remapper remap=new Remapper(Opcodes.ASM9){public String map(String name){return relocated.contains(name)?"io/github/commentipaddon/internal/"+name.substring("com/xhhao/comment/".length()):name;}};
  for(Path p:classes){ClassReader r=new ClassReader(Files.readAllBytes(p));ClassWriter w=new ClassWriter(0);r.accept(new ClassRemapper(w,remap),0);ClassNode fixed=new ClassNode();new ClassReader(w.toByteArray()).accept(fixed,0);for(MethodNode m:fixed.methods)for(AbstractInsnNode instruction:m.instructions)if(instruction instanceof MethodInsnNode call && call.owner.equals("com/xhhao/comment/widget/iplocation/CommentNextIpLocationService") && call.name.equals("<init>"))call.desc=call.desc.replace("io/github/commentipaddon/internal/widget/iplocation/CommentNextIpLocationClient","com/xhhao/comment/widget/iplocation/CommentNextIpLocationClient");w=new ClassWriter(0);fixed.accept(w);Path dest=out.resolve(remap.map(r.getClassName())+".class");Files.createDirectories(dest.getParent());Files.write(dest,w.toByteArray());ClassNode c=new ClassNode();new ClassReader(w.toByteArray()).accept(c,0);for(MethodNode m:c.methods)if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(c.name,m);}
  for(String dir:List.of("org/json","org/lionsoul/ip2region","ip2region")){Path root=Path.of("work/v1013/original/"+dir);try(var f=Files.walk(root)){for(Path p:f.filter(Files::isRegularFile).toList()){Path dest=out.resolve(dir).resolve(root.relativize(p));Files.createDirectories(dest.getParent());Files.copy(p,dest,StandardCopyOption.REPLACE_EXISTING);}}}
  System.out.println("PASS: addon own classes relocated and bytecode checked; upstream service/mapper NOT packaged.");
 }
}

