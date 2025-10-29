import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Example {

    public static void main(String[] args) {
        try {
            System.out.println("부모 프로세스 시작!!");
            // ProcessBuilder를 사용하여 외부 프로그램 실행
            ProcessBuilder processBuilder = new ProcessBuilder("ls", "-l");

            // 자식 프로세스 시작
            System.out.println("자식 프로세스 생성 및 실행!!");
            // start() 호출 시 fork() 후 exec()를 호출한 것과 유사합니다.
            Process childProcess = processBuilder.start();

            // 자식 프로세스 출력 읽기
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            childProcess.getInputStream()));

            String line;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }

            // 자식 프로세스 종료 대기
            int exitCode = childProcess.waitFor();
            System.out.println("자식 프로세스 종료!! : " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}