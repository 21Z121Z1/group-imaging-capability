import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
public class ParseCheck {
  public static void main(String[] args) throws Exception {
    JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> dc=new DiagnosticCollector<>();
    try(StandardJavaFileManager fm=compiler.getStandardFileManager(dc,null,null)){
      List<Path> paths=new ArrayList<>();
      try(var s=Files.walk(Path.of(args[0]))){s.filter(p->p.toString().endsWith(".java")).forEach(paths::add);}
      var files=fm.getJavaFileObjectsFromPaths(paths);
      JavacTask task=(JavacTask)compiler.getTask(null,fm,dc,List.of("-proc:none"),null,files);
      task.parse();
      long errors=dc.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
      for(var d:dc.getDiagnostics()) if(d.getKind()==Diagnostic.Kind.ERROR) System.err.println(d.getSource()+":"+d.getLineNumber()+": "+d.getMessage(null));
      if(errors>0) System.exit(1);
      System.out.println("JAVA PARSE PASSED: "+paths.size()+" files");
    }
  }
}
