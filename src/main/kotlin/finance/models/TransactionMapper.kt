package finance.models

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.sql.SQLException

class TransactionMapper : RowMapper<Transaction> {

    @Throws(SQLException::class)
    override fun mapRow(resultSet: ResultSet, i: Int): Transaction? {

        val transaction = Transaction()

        transaction.transactionId = (resultSet.getLong("transaction_id"))
        transaction.accountNameOwner = (resultSet.getString("account_name_owner"))
        transaction.guid = (resultSet.getString("guid"))
        transaction.description = (resultSet.getString("description"))
        transaction.category = (resultSet.getString("category"))
        transaction.notes = (resultSet.getString("notes"))
        //TODO: fix
        //transaction.accountType = (resultSet.getString("account_type"))
        transaction.amount = (resultSet.getBigDecimal("amount"))
        transaction.cleared = (resultSet.getInt("cleared"))
        transaction.reoccurring = (resultSet.getBoolean("reoccurring"))

        val transaction_date = resultSet.getDate("transaction_date")

        return transaction
    }
}
