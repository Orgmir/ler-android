package app.luisramos.thecollector.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.luisramos.thecollector.R
import kotlinx.android.synthetic.main.item_stacked_labels.view.*

class StackedLabelsAdapter : RecyclerView.Adapter<StackedLabelsAdapter.ViewHolder>() {

    var items: List<Pair<String, String>> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_stacked_labels, parent, false
            )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (text1, text2) = items[position]
        holder.itemView.apply {
            textView1.text = text1
            textView2.text = text2
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}