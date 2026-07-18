package com.project.farmingapp.view.articles
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.adapter.ArticleListAdapter
import com.project.farmingapp.model.data.OfflineAgricultureArticle
import com.project.farmingapp.model.data.OfflineAgricultureArticles
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.viewmodel.ArticleViewModel


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ArticleListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ArticleListFragment : Fragment(), CellClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewModel: ArticleViewModel
    lateinit var Adapter: ArticleListAdapter
    lateinit var fruitFragment: FruitsFragment
    private var recycler: androidx.recyclerview.widget.RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[ArticleViewModel::class.java]

        viewModel.getAllArticles("article_fruits")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_article_list, container, false)
        recycler = root.findViewById(R.id.recyclerArticleListFrag)

        viewModel.message3.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No articles found. Showing offline mode.", Toast.LENGTH_SHORT).show()
                showOfflineArticles()
                return@Observer
            }
            Log.d("Art All Data", it[0].data.toString())

            Adapter = ArticleListAdapter(requireContext(), it, this)
            recycler?.adapter = Adapter
            recycler?.layoutManager = GridLayoutManager(requireContext(), 2)

        })

        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ArticleListFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ArticleListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Articles"

    }



    override fun onCellClickListener(name: String) {
        fruitFragment = FruitsFragment()
        val bundle =
            OfflineAgricultureArticles.bundleForTitle(name) ?: Bundle().apply { putString("name", name) }
        fruitFragment.setArguments(bundle)
        val transaction = activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, fruitFragment, name)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .setReorderingAllowed(true)
            .addToBackStack("name")
            .commit()
    }

    private fun showOfflineArticles() {
        recycler?.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler?.adapter = OfflineArticleAdapter(OfflineAgricultureArticles.all) { item ->
            onCellClickListener(item.title)
        }
    }

    class OfflineArticleAdapter(
        private val items: List<OfflineAgricultureArticle>,
        private val onClick: (OfflineAgricultureArticle) -> Unit
    ) : RecyclerView.Adapter<OfflineArticleAdapter.OfflineArticleVH>() {
        class OfflineArticleVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title = itemView.findViewById<android.widget.TextView>(R.id.descTextxArticleListFrag)
            val image = itemView.findViewById<android.widget.ImageView>(R.id.imageArticleListFrag)
            val card = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.articleListCardArtListFrag)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineArticleVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.article_list_single, parent, false)
            return OfflineArticleVH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: OfflineArticleVH, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            Glide.with(holder.itemView)
                .load(AgriAssetPaths.fileUri(item.imageAsset))
                .into(holder.image)
            holder.card.setOnClickListener { onClick(item) }
        }
    }
}