import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 시스템 호출(System Call) 예제
 * 사용자 모드에서 커널 모드로 전환하여 파일 시스템에 접근하는 과정을 보여줍니다.
 */
public class SysCall {
    
    // 1. 사용자 모드에서 프로그램 실행
    public static void main(String[] args) {
        String filePath = "example.txt";

        // 2. BufferedReader와 FileReader를 통해 파일을 읽는 Java 코드 실행
        // 3. FileReader 클래스가 Java의 표준 API를 통해 파일 시스템 접근 요청
        // 4. FileReader 클래스에서 내부적으로 "open()" 시스템 호출!!
        // 5. 파일을 buffer(다발 형태)로 JVM에 반환
        try (BufferedReader reader = new BufferedReader(
                new FileReader(filePath))) {

            String line;
            // 7. 버퍼로 파일을 한줄 한줄 읽으며 "read()" 시스템 호출!!
            // 8. 읽어들인 문자열을 JVM에 반환
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // 파일 내용 출력
            }
        } catch (IOException e) {
            e.printStackTrace(); // 예외 처리
        }

        // 9. try-catch 후 auto-closed하여 "close()" 시스템 호출!!
    }
}