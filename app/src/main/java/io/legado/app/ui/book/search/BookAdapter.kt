package io.legado.app.ui.book.search

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import kotlinx.android.synthetic.main.item_text.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class BookAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_text) {

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            text_view.text = item.name
            onClick { callBack.showBookInfo(item) }
        }
    }

    interface CallBack {
        fun showBookInfo(book: Book)
    }
}