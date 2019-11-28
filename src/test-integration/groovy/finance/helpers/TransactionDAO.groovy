package finance.helpers


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class TransactionDAO {
    private String SQL_TRUNCATE_TRANSACTION_TABLE = "TRUNCATE TABLE t_transaction CASCADE"
    private String SQL_DROP_TRANSACTION_TABLE = "DROP TABLE t_transaction"
    private String SQL_TRUNCATE_ACCOUNT_TABLE = "TRUNCATE TABLE t_account CASCADE"
    private String SQL_DROP_ACCOUNT_TABLE = "DROP TABLE t_account"
    private String SQL_TRUNCATE_CATEGORY_TABLE = "TRUNCATE TABLE t_category CASCADE"
    private String SQL_DROP_CATEGORY_TABLE = "DROP TABLE t_category"
    private String SQL_TRUNCATE_TRANSACTION_CATEGORIES_TABLE = "TRUNCATE TABLE t_transaction_categories"
    private String SQL_DROP_TRANSACTION_CATEGORIES_TABLE = "DROP TABLE t_transaction_categories"

    @Autowired
    private JdbcTemplate jdbcTemplate

    void truncateTransactionTable() {
        jdbcTemplate.execute(this.SQL_TRUNCATE_TRANSACTION_TABLE)
    }

    void dropTransactionTable() {
        jdbcTemplate.execute(this.SQL_DROP_TRANSACTION_TABLE)
    }

    void truncateTransactionCategories() {
        jdbcTemplate.execute(this.SQL_TRUNCATE_TRANSACTION_CATEGORIES_TABLE)
    }

    void dropTransactionCategories() {
        jdbcTemplate.execute(this.SQL_DROP_TRANSACTION_CATEGORIES_TABLE)
    }

    void dropAccountTable() {
        jdbcTemplate.execute(this.SQL_DROP_ACCOUNT_TABLE)
    }

    void truncateAccountTable() {
        jdbcTemplate.execute(this.SQL_TRUNCATE_ACCOUNT_TABLE)
    }

    void dropCategoryTable() {
        jdbcTemplate.execute(this.SQL_DROP_CATEGORY_TABLE)
    }

    void truncateCategoryTable() {
        jdbcTemplate.execute(this.SQL_TRUNCATE_CATEGORY_TABLE)
    }

    Integer transactionCount()  {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_transaction", Ineger) as Integer
    }

    Integer transactionCategoriesCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_transaction_categories", Integera) as Integer
    }
}