package developer.abdusamid.firebasefirestore

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import developer.abdusamid.firebasefirestore.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val personCollectionRef = Firebase.firestore.collection("persons")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            btnUploadData.setOnClickListener {
                val person = getOldPerson()
                savePerson(person)
            }
            btnUpdatePerson.setOnClickListener {
                val oldPerson = getOldPerson()
                val newPersonMap = getNewPersonMap()
                upDatePerson(oldPerson, newPersonMap)
            }
            btnDeletePerson.setOnClickListener {
                val oldPerson = getOldPerson()
                deletePerson(oldPerson)
            }
            btnRetrieveData.setOnClickListener {
                retrievePersons()
            }
        }
    }

    private fun getOldPerson(): Person {
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val age = binding.etAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = binding.etNewFirstName.text.toString()
        val lastName = binding.etNewLastName.text.toString()
        val age = binding.etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if (firstName.isNotEmpty()) map["firstName"] = firstName
        if (lastName.isNotEmpty()) map["lastName"] = lastName
        if (age.isNotEmpty()) map["age"] = age.toInt()
        return map
    }

    private fun subscribeToRealtimeUpdates() {
        personCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                binding.tvPersons.text = sb.toString()
            }
        }
    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get().await()
        if (personQuery.documents.isNotEmpty()) {
            for (documents in personQuery) {
                try {
                    personCollectionRef.document(documents.id).delete().await()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No Person Matched The Query", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun upDatePerson(person: Person, newPersonMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = personCollectionRef
                .whereEqualTo("firstName", person.firstName)
                .whereEqualTo("lastName", person.lastName)
                .whereEqualTo("age", person.age)
                .get()
                .await()
            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        personCollectionRef.document(document.id)
                            .set(newPersonMap, SetOptions.merge()).await()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No Persons Matched The Query",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun retrievePersons() = CoroutineScope(Dispatchers.IO).launch {
        val fromAge = binding.etFrom.text.toString().toInt()
        val toAge = binding.etTo.text.toString().toInt()
        try {
            val querySnapshot = personCollectionRef
                .whereGreaterThan("age", fromAge)
                .whereLessThan("age", toAge)
                .orderBy("age")
                .get()
                .await()
            val sb = StringBuilder()
            for (document in querySnapshot.documents) {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main) {
                binding.tvPersons.text = sb.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully Saved Data", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}