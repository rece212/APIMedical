package vcnmb.rwanvig.apimedical

data class Loan(
    val loanID: Int,
    val amount: String,
    val memberID: String,
    val message: String
)
data class LoanPost(
    val Amount: String,
    val MemberID: String,
    val Message: String
)