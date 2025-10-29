class Person {
    private String name; // 힙 영역: 객체의 인스턴스 변수
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 코드 영역: 메서드 정의
    public int work(int a, int b) {
        // 스택 영역: 매개변수 a, b와 지역 변수 result
        int result = a + b;
        return result;
    }

    public String getName() {
        return this.name;
    }

    public int getAge() {
        return this.age;
    }
}

public class MemoryExample {

    // 데이터 영역: 정적 변수 (Static 변수)
    public static int STATIC_VAR = 10;

    // 코드 영역: 메서드 정의
    public static void main(String[] args) {

        // 스택 영역: 지역 변수
        int localVar = 20;

        // 힙 영역: 객체 생성
        Person person = new Person("태현", 10);

        // 메서드 호출 (스택에 추가됨)
        int sum = person.work(localVar, STATIC_VAR);

        System.out.println("Sum: " + sum);

        System.out.println("Person: " + person.getName() + ", Age: " + person.getAge());
    }
}