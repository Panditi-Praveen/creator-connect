import Swal from 'sweetalert2'

/**
 * Single notification helper for the app — wraps SweetAlert2 with the
 * CreatorConnect brand styling so dialogs stay consistent everywhere.
 */

const BRAND = {
  background: '#ffffff',
  color: '#1e293b',
  confirmButtonColor: '#4f46e5',
  cancelButtonColor: '#64748b',
  customClass: {
    popup: 'cc-swal',
    confirmButton: 'cc-swal-confirm',
    cancelButton: 'cc-swal-cancel',
  },
}

export function swalSuccess(
  title: string,
  text: string,
  confirmText = 'OK',
): Promise<{ isConfirmed: boolean }> {
  return Swal.fire({
    title,
    text,
    icon: 'success',
    confirmButtonText: confirmText,
    ...BRAND,
  })
}

export function swalError(title: string, text: string): Promise<{ isConfirmed: boolean }> {
  return Swal.fire({
    title,
    text,
    icon: 'error',
    confirmButtonText: 'OK',
    ...BRAND,
  })
}

/** Promise resolves to true when the user confirmed, false otherwise. */
export async function swalConfirm(
  title: string,
  text: string,
  confirmText = 'Confirm',
): Promise<boolean> {
  const result = await Swal.fire({
    title,
    text,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: confirmText,
    cancelButtonText: 'Cancel',
    reverseButtons: true,
    ...BRAND,
  })
  return result.isConfirmed === true
}
