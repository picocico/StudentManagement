package raisetech.student.management;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/student")
public class Application {

  @Autowired
  private StudentRepository repository;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  // 全学生のリストを取得
  @GetMapping("/all")
  public List<Student>getAllStudents() {
    return repository.findAll();
  }

  // 名前で検索
  @GetMapping("/searchByName")
  public Student getStudentByName(@RequestParam String name) {
    return repository.searchByName(name);
  }

  // IDで検索
  @GetMapping("/searchById")
  public Student getStudentById(@RequestParam int id) {
    return repository.searchById(id);
  }

  // 新しい学生を登録
  @PostMapping("/register")
  public void registerStudent(@RequestParam String name, @RequestParam int age) {
    repository.registerStudent(name, age);
  }

  // IDで学生を更新
  @PatchMapping("/update")
  public void updateStudent(@RequestParam int id, @RequestParam String name, @RequestParam int age) {
    repository.updateStudent(id, name, age);
  }

  // 名前で削除
  @DeleteMapping("/deleteByName")
  public void deleteStudentByName(@RequestParam String name) {
    repository.deleteStudentByName(name);
  }

  // IDで削除
  @DeleteMapping("/deleteById")
  public void deleteStudentById(@RequestParam int id) {
    repository.deleteStudentById(id);
  }
}


