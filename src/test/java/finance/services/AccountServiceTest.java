package finance.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AccountServiceTest {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    //@Mock
    //AccountRepository accountsRepository;

    //@InjectMocks
    //AccountService accountService;

    @Before
    public void setUp() {
    }

    @Test
    public void findAll()  {
        assert (true);
    }
}