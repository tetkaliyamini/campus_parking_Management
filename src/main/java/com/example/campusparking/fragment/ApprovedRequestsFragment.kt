package com.example.campusparking.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusparking.adapter.ParkingRequestAdapter
import com.example.campusparking.databinding.FragmentPendingRequestsBinding
import com.example.campusparking.db.DatabaseHelper

class ApprovedRequestsFragment : Fragment() {

    private var _binding: FragmentPendingRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        loadApprovedRequests()
    }

    private fun loadApprovedRequests() {
        val approvedRequests = dbHelper.getApprovedRequests()

        if (approvedRequests.isEmpty()) {
            binding.tvNoRequests.text = "No approved requests"
            binding.tvNoRequests.visibility = View.VISIBLE
            binding.rvPendingRequests.visibility = View.GONE
        } else {
            binding.tvNoRequests.visibility = View.GONE
            binding.rvPendingRequests.visibility = View.VISIBLE

            val adapter = ParkingRequestAdapter(approvedRequests, dbHelper, false)
            binding.rvPendingRequests.layoutManager = LinearLayoutManager(requireContext())
            binding.rvPendingRequests.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

