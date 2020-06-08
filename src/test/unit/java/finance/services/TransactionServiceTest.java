//package finance.services;
//
//import finance.domain.Account;
//import finance.domain.Category;
//import finance.domain.Transaction;
//import finance.repositories.AccountRepository;
//import finance.repositories.CategoryRepository;
//import finance.repositories.TransactionRepository;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Optional;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.ArgumentMatchers.anyString;
//
//public class TransactionServiceTest {
//    //private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
//
//    @InjectMocks
//    TransactionService transactionService;
//
//    @Mock
//    private AccountRepository accountRepository;
//
//    @Mock
//    private TransactionRepository transactionRepository;
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    //@Mock
//    //TransactionDAO transactionDAO;
//
//    //@Mock
//    //JdbcTemplate jdbcTemplate;
//
//    //@Inject private DatabaseAccessClass dao;
//
//    @Before
//    public void setUp() {
//        //transaction.setAccountNameOwner("discover_brian");
//        MockitoAnnotations.initMocks(this);
//        //Mockito.when(jdbcTemplate.update(anyString(),anyObject())).thenReturn(1);
//        //Mockito.when(jdbcTemplate.update(anyString(),Mockito.any(Object[].class))).thenReturn(1);
//        //Mockito.when(jdbcTemplate.update(anyString(), MapSqlParameterSource[].class)).thenReturn(1);
//    }
//
////    @Test
////    public void deleteByGuidTest() {
////        Transaction transaction = new Transaction();
////        Optional<Transaction> transactionOptional = Optional.of(transaction);
////
////        Mockito.doNothing().when(transactionRepository).deleteByGuid(anyString());
////        Mockito.when(transactionRepository.findByGuid(anyString())).thenReturn(transactionOptional);
////
////        boolean isDeleted = transactionService.deleteByGuid("123");
////        assertTrue(isDeleted);
////    }
//
////    @Test
////    public void deleteByGuidNoRecordTest() {
////        //Transaction transaction = new Transaction();
////        Optional<Transaction> transactionOptional = Optional.empty();
////
////        Mockito.doNothing().when(transactionRepository).deleteByGuid(anyString());
////        Mockito.when(transactionRepository.findByGuid(anyString())).thenReturn(transactionOptional);
////
////        boolean isDeleted = transactionService.deleteByGuid("123");
////
////        assertFalse(isDeleted);
////    }
//
////    @Test
////    public void insertTransactionTest() {
////        Transaction transaction = new Transaction();
////        Account account = new Account();
////        Category category = new Category();
////        //Optional<Transaction> transactionOptional = Optional.of(transaction);
////        Optional<Account> accountOptional = Optional.of(account);
////        Optional<Category> categoryOptional = Optional.of(category);
////
////        Mockito.when(categoryRepository.findByCategory(Mockito.anyString())).thenReturn(categoryOptional);
////        Mockito.when(accountRepository.findByAccountNameOwner(Mockito.anyString())).thenReturn(accountOptional);
////        Mockito.when(transactionRepository.saveAndFlush(Mockito.any())).thenReturn(transaction);
////        boolean isInserted = transactionService.insertTransaction(transaction);
////        assertTrue(isInserted);
////    }
//
////    @Test
////    public void insertTransactionNoAccountTest() {
////        Transaction transaction = new Transaction();
////        //Account account = new Account();
////        Category category = new Category();
////        //Optional<Transaction> transactionOptional = Optional.of(transaction);
////        Optional<Account> accountOptional = Optional.empty();
////        Optional<Category> categoryOptional = Optional.of(category);
////
////        Mockito.when(categoryRepository.findByCategory(Mockito.anyString())).thenReturn(categoryOptional);
////        Mockito.when(accountRepository.findByAccountNameOwner(Mockito.anyString())).thenReturn(accountOptional);
////        Mockito.when(transactionRepository.saveAndFlush(Mockito.any())).thenReturn(transaction);
////        boolean isInserted = transactionService.insertTransaction(transaction);
////        assertFalse(isInserted);
////    }
//
//    @Test
//    public void insertTransactionNoTransactionTest() {
//        Transaction transaction = new Transaction();
//        Account account = new Account();
//        Category category = new Category();
//        //Optional<Transaction> transactionOptional = Optional.empty();
//        Optional<Account> accountOptional = Optional.of(account);
//        Optional<Category> categoryOptional = Optional.of(category);
//
//        Mockito.when(categoryRepository.findByCategory(Mockito.anyString())).thenReturn(categoryOptional);
//        Mockito.when(accountRepository.findByAccountNameOwner(Mockito.anyString())).thenReturn(accountOptional);
//        Mockito.when(transactionRepository.saveAndFlush(Mockito.any())).thenReturn(transaction);
//        boolean isInserted = transactionService.insertTransaction(transaction);
//        assertTrue(isInserted);
//    }
//
////    @Test
////    public void findByGuidTest() {
////        Transaction transaction = new Transaction();
////        Optional<Transaction> transactionOptional = Optional.of(transaction);
////
////        Mockito.when(transactionRepository.findByGuid(anyString())).thenReturn(transactionOptional);
////        transactionService.findByGuid("123");
////        assert(true);
////    }
//}
