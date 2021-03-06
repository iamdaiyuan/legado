package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.ImportOldData
import io.legado.app.help.storage.Restore
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentPath
import io.legado.app.utils.toast
import kotlinx.coroutines.Dispatchers.Main
import org.jetbrains.anko.toast

object BackupRestoreUi {

    private const val backupSelectRequestCode = 22
    private const val restoreSelectRequestCode = 33
    private const val oldDataRequestCode = 11

    fun backup(fragment: Fragment) {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            selectBackupFolder(fragment)
        } else {
            if (backupPath.isContentPath()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(fragment.requireContext(), uri)
                if (doc?.canWrite() == true) {
                    Coroutine.async {
                        Backup.backup(fragment.requireContext(), backupPath)
                    }.onSuccess {
                        fragment.toast(R.string.backup_success)
                    }
                } else {
                    selectBackupFolder(fragment)
                }
            } else {
                backupUsePermission(fragment)
            }
        }
    }

    private fun backupUsePermission(fragment: Fragment, path: String = Backup.legadoPath) {
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = Backup.legadoPath
                    Backup.backup(fragment.requireContext(), path)
                }.onSuccess {
                    fragment.toast(R.string.backup_success)
                }
            }
            .request()
    }

    fun selectBackupFolder(fragment: Fragment) {
        fragment.alert {
            titleResource = R.string.select_folder
            items(fragment.resources.getStringArray(R.array.select_folder).toList()) { _, index ->
                when (index) {
                    0 -> backupUsePermission(fragment)
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            fragment.startActivityForResult(intent, backupSelectRequestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {
                        FileChooserDialog.show(
                            fragment.childFragmentManager,
                            backupSelectRequestCode,
                            mode = FileChooserDialog.DIRECTORY
                        )
                    }
                }
            }
        }.show()
    }

    fun restore(fragment: Fragment) {
        Coroutine.async(context = Main) {
            if (!WebDavHelp.showRestoreDialog(fragment.requireContext()) {
                    fragment.toast(R.string.restore_success)
                }) {
                val backupPath = fragment.getPrefString(PreferKey.backupPath)
                if (backupPath?.isNotEmpty() == true) {
                    if (backupPath.isContentPath()) {
                        val uri = Uri.parse(backupPath)
                        val doc = DocumentFile.fromTreeUri(fragment.requireContext(), uri)
                        if (doc?.canWrite() == true) {
                            Restore.restore(fragment.requireContext(), backupPath)
                            fragment.toast(R.string.restore_success)
                        } else {
                            selectRestoreFolder(fragment)
                        }
                    } else {
                        restoreUsePermission(fragment, backupPath)
                    }
                } else {
                    selectRestoreFolder(fragment)
                }
            }
        }
    }

    private fun restoreUsePermission(fragment: Fragment, path: String = Backup.legadoPath) {
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Restore.restore(path)
                }.onSuccess {
                    fragment.toast(R.string.restore_success)
                }
            }
            .request()
    }

    private fun selectRestoreFolder(fragment: Fragment) {
        fragment.alert {
            titleResource = R.string.select_folder
            items(fragment.resources.getStringArray(R.array.select_folder).toList()) { _, index ->
                when (index) {
                    0 -> restoreUsePermission(fragment)
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            fragment.startActivityForResult(intent, restoreSelectRequestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {
                        FileChooserDialog.show(
                            fragment.childFragmentManager,
                            restoreSelectRequestCode,
                            mode = FileChooserDialog.DIRECTORY
                        )
                    }
                }
            }
        }.show()
    }

    fun importOldData(fragment: Fragment) {
        fragment.alert {
            titleResource = R.string.select_folder
            items(fragment.resources.getStringArray(R.array.select_folder).toList()) { _, index ->
                when (index) {
                    0 -> importOldUsePermission(fragment)
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            fragment.startActivityForResult(intent, oldDataRequestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {
                        FileChooserDialog.show(
                            fragment.childFragmentManager,
                            oldDataRequestCode,
                            mode = FileChooserDialog.DIRECTORY
                        )
                    }
                }
            }
        }.show()
    }

    private fun importOldUsePermission(fragment: Fragment) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            fragment.toast(R.string.a10_permission_toast)
        }
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                ImportOldData.import(fragment.requireContext())
            }
            .request()
    }

    fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            backupSelectRequestCode -> {
                AppConfig.backupPath = currentPath
                Coroutine.async {
                    Backup.backup(App.INSTANCE, currentPath)
                }.onSuccess {
                    App.INSTANCE.toast(R.string.backup_success)
                }
            }
            restoreSelectRequestCode -> {
                AppConfig.backupPath = currentPath
                Coroutine.async {
                    Restore.restore(App.INSTANCE, currentPath)
                }.onSuccess {
                    App.INSTANCE.toast(R.string.restore_success)
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            backupSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    App.INSTANCE.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.backupPath = uri.toString()
                    Coroutine.async {
                        Backup.backup(App.INSTANCE, uri.toString())
                    }.onSuccess {
                        App.INSTANCE.toast(R.string.backup_success)
                    }
                }
            }
            restoreSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    App.INSTANCE.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.backupPath = uri.toString()
                    Coroutine.async {
                        Restore.restore(App.INSTANCE, uri.toString())
                    }.onSuccess {
                        App.INSTANCE.toast(R.string.restore_success)
                    }
                }
            }
            oldDataRequestCode ->
                if (resultCode == RESULT_OK) data?.data?.let { uri ->
                    ImportOldData.importUri(uri)
                }
        }
    }

}