package vcnmb.rwanvig.apimedical

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var outputTextView: TextView
    private lateinit var inputIdEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        outputTextView = findViewById(R.id.txtOutput)
        inputIdEditText = findViewById(R.id.etInputId)
        findViewById<Button>(R.id.btnGetAll).setOnClickListener {
            hideKeyboard()
            getAllLoans()
        }
        findViewById<Button>(R.id.btnGetById).setOnClickListener {
            hideKeyboard()
            val idText = inputIdEditText.text.toString()
            if (idText.isNotEmpty()) {
                try {
                    // Try to convert the input to an Integer.
                    val id = idText.toInt()
                    getLoanById(id)
                } catch (e: NumberFormatException) {
                    // Handle cases where the input is not a valid number.
                    Toast.makeText(this, "Please enter a valid numeric Loan ID."
                        , Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a Loan ID."
                    , Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            hideKeyboard()
            createNewLoan()
        }
        findViewById<Button>(R.id.btnGetByMember).setOnClickListener {
            hideKeyboard()
            val memberId = inputIdEditText.text.toString()
            if (memberId.isNotEmpty()) {
                getLoansByMemberId(memberId)
            } else {
                Toast.makeText(this, "Please enter a Member ID.", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            hideKeyboard()
            val idText = inputIdEditText.text.toString()
            if (idText.isNotEmpty()) {
                try {
                    val id = idText.toInt()
                    deleteLoanById(id)
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid numeric Loan ID.",
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a Loan ID.", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun getAllLoans() {
        // Define the API endpoint URL.
        val url = "https://opsc.azurewebsites.net/loans/"
        outputTextView.text = "Fetching all loans..."

        // Execute the network request on a background thread.
        executor.execute {
            // Use Fuel's httpGet for a GET request.
            url.httpGet().responseString { _, _, result ->
                // Switch to the main thread to update the UI.
                handler.post {
                    when (result) {
                        is com.github.kittinunf.result.Result.Success -> {
                            // On success, deserialize the JSON string into a list of Loan objects.
                            val json = result.get()
                            try {
                                val loans = Gson().fromJson(json, Array<Loan>::class.java).toList()
                                if (loans.isNotEmpty()) {
                                    // Format the output for readability.
                                    val formattedOutput = loans.joinToString(separator = "\n\n") { loan ->
                                        "Loan ID: ${loan.loanID}\nAmount: ${loan.amount}\nMember ID: " +
                                                "${loan.memberID}\nMessage: ${loan.message}"
                                    }
                                    outputTextView.text = formattedOutput
                                } else {
                                    outputTextView.text = "No loans found."
                                }
                            } catch (e: JsonSyntaxException) {
                                // Handle cases where the server response is not valid JSON.
                                Log.e("GetAllLoans", "JSON parsing error: ${e.message}")
                                outputTextView.text = "Error: Could not parse server response."
                            }
                        }
                        is Result.Failure -> {
                            // On failure, log the error and show a user-friendly message.
                            val ex = result.getException()
                            Log.e("GetAllLoans", "API Error: ${ex.message}")
                            outputTextView.text = "Error: Could not fetch loans from the server."
                        }
                    }
                }
            }
        }
    }
    private fun getLoanById(id: Int) {
        val url = "https://opsc.azurewebsites.net/loans/$id"
        outputTextView.text = "Fetching loan with ID: $id..."

        executor.execute {
            url.httpGet().responseString { _, response, result ->
                handler.post {
                    if (response.statusCode == 404) {
                        outputTextView.text = "Loan with ID $id not found."
                        return@post
                    }

                    when (result) {
                        is Result.Success -> {
                            try {
                                val loan = Gson().fromJson(result.get(), Loan::class.java)
                                val formattedOutput = "Loan ID: ${loan.loanID}\nAmount: ${loan.amount}" +
                                        "\nMember ID: ${loan.memberID}\nMessage: ${loan.message}"
                                outputTextView.text = formattedOutput
                            } catch (e: JsonSyntaxException) {
                                Log.e("GetLoanById", "JSON parsing error: ${e.message}")
                                outputTextView.text = "Error: Could not parse server response."
                            }
                        }
                        is Result.Failure -> {
                            val ex = result.getException()
                            Log.e("GetLoanById", "API Error: ${ex.message}")
                            outputTextView.text = "Error: Could not fetch loan."
                        }
                    }
                }
            }
        }
    }
    private fun getLoansByMemberId(memberId: String) {
        val url = "https://opsc.azurewebsites.net/loans/member/$memberId"
        outputTextView.text = "Fetching loans for member: $memberId..."
        executor.execute {
            url.httpGet().responseString { _, _, result ->
                handler.post {
                    when (result) {
                        is Result.Success -> {
                            try {
                                val loans = Gson().fromJson(result.get(),
                                    Array<Loan>::class.java).toList()
                                if (loans.isNotEmpty()) {
                                    val formattedOutput = loans.joinToString(separator = "\n\n") { loan ->
                                        "Loan ID: ${loan.loanID}\nAmount: ${loan.amount}\nMember ID:" +
                                                " ${loan.memberID}\nMessage: ${loan.message}"
                                    }
                                    outputTextView.text = formattedOutput
                                } else {
                                    outputTextView.text = "No loans found for member $memberId."
                                }
                            } catch (e: JsonSyntaxException) {
                                Log.e("GetLoansByMemberId", "JSON parsing error: ${e.message}")
                                outputTextView.text = "Error: Could not parse server response."
                            }
                        }
                        is Result.Failure -> {
                            val ex = result.getException()
                            Log.e("GetLoansByMemberId", "API Error: ${ex.message}")
                            outputTextView.text = "Error: Could not fetch loans."
                        }
                    }
                }
            }
        }
    }
    private fun createNewLoan() {
        val url = "https://opsc.azurewebsites.net/loans/"
        outputTextView.text = "Creating a new loan..."
        executor.execute {
            // Create a new loan object to send as the request body.
            val newLoan = LoanPost("15.99", "M6001",
                "Added by the Android app")
            val jsonBody = Gson().toJson(newLoan)

            url.httpPost()
                .jsonBody(jsonBody) // Set the request body.
                .responseString { _, response, result ->
                    handler.post {
                        when (result) {
                            // A 201 Created status code indicates success.
                            is Result.Success -> {
                                if (response.statusCode == 201) {
                                    try {
                                        val createdLoan = Gson().fromJson(result.get(),
                                            Loan::class.java)
                                        outputTextView.text = "Successfully created loan:\n\nLoan ID:" +
                                                " ${createdLoan.loanID}\nAmount: ${createdLoan.amount}"
                                    } catch (e: JsonSyntaxException) {
                                        Log.e("CreateNewLoan", "JSON parsing error: ${e.message}")
                                        outputTextView.text = "Loan created, but failed to parse response."
                                    }
                                } else {
                                    outputTextView.text = "Failed to create loan. Status: ${response.statusCode}"
                                }
                            }
                            is Result.Failure -> {
                                val ex = result.getException()
                                Log.e("CreateNewLoan", "API Error: ${ex.message}")
                                outputTextView.text = "Error: Could not create loan."
                            }
                        }
                    }
                }
        }
    }
    private fun deleteLoanById(id: Int) {
        val url = "https://opsc.azurewebsites.net/loans/$id"
        outputTextView.text = "Deleting loan with ID: $id..."

        executor.execute {
            url.httpDelete().response { _, response, _ ->
                handler.post {
                    // A 204 No Content status code means the deletion was successful.
                    when (response.statusCode) {
                        204 -> {
                            outputTextView.text = "Successfully deleted loan with ID: $id"
                        }
                        404 -> {
                            outputTextView.text = "Loan with ID $id not found."
                        }
                        else -> {
                            Log.e("DeleteLoanById", "API Error: Status code ${response.statusCode}")
                            outputTextView.text = "Error: Could not delete loan. Status: ${response.statusCode}"
                        }
                    }
                }
            }
        }
    }
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputIdEditText.windowToken, 0)
    }
}