package finance.dao

import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

@Component
open class TransactionDAO {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate;

    private val SQL_FIND_TRANSACTION = "SELECT * FROM t_transaction WHERE transaction_id = ?"
    private val SQL_FIND_TRANSACTION_BY_ACCOUNT_NAME_OWNER = "SELECT * FROM t_transaction WHERE account_name_owner = ?"
    private val SQL_UPDATE_UPDATE_TS = "UPDATE t_transaction set date_u=''"
    private val SQL_DELETE_ALL_TRANSACTIONS = "DELETE FROM t_transaction"
    private val SQL_DELETE_TRANSACTION_BY_GUID = "DELETE FROM t_transaction WHERE guid = ?"
    private val SQL_CREATE_TABLE = "CREATE TABLE t_category_test(category_id INTEGER NOT NULL, category VARCHAR(50))"

    //https://hackernoon.com/spring-5-jdbc-support-for-kotlin-7cc31f4db4a5

//    fun getTransactionByAccountNameOwner(accountNameOwner: String): List<Transaction> {
//        return jdbcTemplate.query(SQL_FIND_TRANSACTION_BY_ACCOUNT_NAME_OWNER, arrayOf<Any>(accountNameOwner), TransactionMapper())
//    }

    @Timed
    fun deleteTransactionByGuid(guid: String): Int {
        val params = MapSqlParameterSource().addValue("guid", guid)

        return jdbcTemplate.update(this.SQL_DELETE_TRANSACTION_BY_GUID, arrayOf<Any>(params))
    }

    @Timed
    fun deleteAll(): Int {
        return jdbcTemplate.update("delete from t_transaction")
    }

    @Timed
    fun createCategoryTestTable() {
        jdbcTemplate.execute(this.SQL_CREATE_TABLE)
    }
}
