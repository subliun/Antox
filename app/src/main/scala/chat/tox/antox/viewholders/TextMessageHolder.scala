package chat.tox.antox.viewholders

import android.app.AlertDialog
import android.content.{ClipData, ClipboardManager, Context, DialogInterface}
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.TextView
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.utils.UiUtils
import chat.tox.antox.wrapper.MessageType
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

class TextMessageHolder(val view: View) extends GenericMessageHolder(view) with OnLongClickListener {

  protected val messageTitle = view.findViewById(R.id.message_title).asInstanceOf[TextView]

  def setText(s: String): Unit = {
    messageText.setText(s)
    messageText.setOnLongClickListener(this)

    // Reset the visibility for non-group messages
    messageTitle.setVisibility(View.GONE)
    if (msg.isMine) {
      if (shouldGreentext(s)) {
        messageText.setTextColor(context.getResources.getColor(R.color.green_light))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.white))
      }
    } else {
      if (shouldGreentext(s)) {
        messageText.setTextColor(context.getResources.getColor(R.color.green))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.black))
      }
      if (msg.`type` == MessageType.GROUP_PEER) {
        groupMessage()
      }
    }
  }

  def groupMessage() {
    messageText.setText(msg.message)
    messageTitle.setText(msg.senderName)

    if (!msg.received) {
      setAlpha(bubble, 0.5f)
    }
    else {
      setAlpha(bubble, 1f)
    }
    // generate name colour from hash to ensure names have consistent colours
    UiUtils.generateColor(msg.senderName.hashCode)
    if (lastMsg.isEmpty  || msg.senderName != lastMsg.get.senderName) {
      messageTitle.setVisibility(View.VISIBLE)
    }
    messageTitle.setTextColor(UiUtils.generateColor(msg.senderName.hashCode))
    contactMessage()
  }

  private def shouldGreentext(message: String): Boolean = {
    message.startsWith(">")
  }

  override def onLongClick(view: View): Boolean = {
    val items = Array[CharSequence](context.getResources.getString(R.string.message_copy), context.getResources.getString(R.string.message_delete))
    new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, index: Int): Unit = index match {
        case 0 =>
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
          clipboard.setPrimaryClip(ClipData.newPlainText(null, msg.message))

        case 1 =>
          Observable[Boolean](subscriber => {
            val db = State.db
            db.deleteMessage(msg.id)
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()
      }

    }).create().show()

    true
  }

}
