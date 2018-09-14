@file:Suppress("MemberVisibilityCanBePrivate")

package net.samystudio.beaver.ui.base.fragment

//import com.evernote.android.state.State
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import net.samystudio.beaver.R
import net.samystudio.beaver.ui.base.activity.BaseActivity
import net.samystudio.beaver.ui.common.dialog.DialogListener
import net.samystudio.beaver.ui.common.navigation.Navigable

/**
 * Simple fragment with no injection or view model to avoid boilerplate with screen/dialog that
 * display really simple static content. If you want injection or view model, base class is
 * [BaseViewModelFragment].
 */
abstract class BaseFragment : AppCompatDialogFragment(), DialogInterface.OnShowListener, Navigable {
    private var finished: Boolean = false
    private var dialogDismissed: Boolean = false
    private var restoringState: Boolean = false
    @get:LayoutRes
    protected abstract val layoutViewRes: Int
    protected open lateinit var firebaseAnalytics: FirebaseAnalytics
    // @State
    protected var resultCode: Int = Activity.RESULT_CANCELED
    // @State
    protected var resultIntent: Intent? = null
    override lateinit var navigationController: NavController
    // @State
    open var title: String? = null
        set(value) {
            value?.let { activity?.title = it }
            field = value
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (layoutViewRes > 0) inflater.inflate(layoutViewRes, container, false)
        else null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogDismissed = false
    }

    @CallSuper
    open fun onNewIntent(intent: Intent) {
        childFragmentManager.fragments.forEach {
            (it as? BaseFragment)?.onNewIntent(intent)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // In most case we'll extends BaseDataFragment and get these initialized with injection, but
        // if we want really simple fragment screen with no data (less boilerplate), we have to
        // initialize these manually.
        if (!::firebaseAnalytics.isInitialized)
            context?.let { firebaseAnalytics = FirebaseAnalytics.getInstance(it) }
        if (!::navigationController.isInitialized)
            navigationController = activity!!.findNavController(R.id.nav_host)

        if (showsDialog) dialog.setOnShowListener(this)

        title?.let { activity?.title = it }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        restoringState = true
    }

    override fun onResume() {
        super.onResume()

        if (!restoringState) {
            firebaseAnalytics.setCurrentScreen(
                activity!!,
                javaClass.simpleName,
                javaClass.simpleName
            )
            restoringState = false
        }
    }

    /**
     * Parent [Activity] wil call this when [Activity.onBackPressed] is called to check if we want
     * to handle this event. Returning true will cancel activity default behaviour, returning false
     * will let [Activity] handle this event.
     * Note: will not be called if [getShowsDialog] is true.
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return false
    }

    /**
     * Get if specified item will be consume from [onOptionsItemSelected], no more action is
     * required here, you'll consume action in [onOptionsItemSelected] and should return the same
     * [Boolean] from both method with the same item.
     * Note: will not be called if [getShowsDialog] is true.
     */
    open fun willConsumeOptionsItem(item: MenuItem): Boolean {
        return false
    }

    @CallSuper
    override fun onShow(dialog: DialogInterface?) {
        (activity as? DialogListener)?.onDialogShow(targetRequestCode)
        (targetFragment as? DialogListener)?.onDialogShow(targetRequestCode)
    }

    @CallSuper
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        (activity as? DialogListener)?.onDialogCancel(targetRequestCode)
        (targetFragment as? DialogListener)?.onDialogCancel(targetRequestCode)

        setResult(Activity.RESULT_CANCELED, null)
    }

    @CallSuper
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        (activity as? DialogListener)?.onDialogDismiss(targetRequestCode)
        (targetFragment as? DialogListener)?.onDialogDismiss(targetRequestCode)

        dialogDismissed = true
        finish()
    }

    fun setTargetRequestCode(requestCode: Int) {
        setTargetFragment(null, requestCode)
    }

    /**
     * @see [android.app.Activity.setResult]
     */
    fun setResult(code: Int, intent: Intent?) {
        resultCode = code
        resultIntent = intent
    }

    /**
     * Same as [android.app.Activity.finish], if [BaseFragment] is a dialog it will be dismissed
     * otherwise [androidx.fragment.app.FragmentManager] stack will pop.
     */
    fun finish() {
        if (finished) return

        if (showsDialog && !dialogDismissed) dismiss()
        else {
            if (!showsDialog) navigationController.popBackStack()

            (activity as? BaseActivity<*>)?.onActivityResult(
                targetRequestCode,
                resultCode,
                resultIntent
            )
            targetFragment?.onActivityResult(targetRequestCode, resultCode, resultIntent)

            finished = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dialogDismissed = true

        dialog?.let {
            it.setOnShowListener(null)
            it.setOnCancelListener(null)
            it.setOnDismissListener(null)
        }
    }
}
