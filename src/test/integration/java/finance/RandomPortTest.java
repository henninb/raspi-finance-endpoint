package finance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@ActiveProfiles("local")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RandomPortTest {

//    @Value("${local.server.port}")
//    protected int localPort;

    @LocalServerPort
    protected int localPort;

    @Test
    public void getPort() {
        System.out.println(localPort);
        assertTrue(localPort > 0 );
    }
}