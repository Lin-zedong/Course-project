# Optimization Notes

This project has been minimally modified to address four issues:

1. **Readable /events** – templates/events.html now renders diffs using `@notificationService.formatDiffPlain(...)`, 
   and common styles were added to static/css/style.css.

2. **Teacher subscription edit** – AdminSubscriptionController now:
   - Prefills the teacher input on GET when subscription subject type is TEACHER.
   - Normalizes input to accept `T-XXXX` or `XXXX`.
   - Updates the Subscription's Subject when teacher code actually changes (using SubjectRepository).

3. **/admin/manage table alignment** – CSS rules were appended for `.admin-table` so headers and cells are 
   center-aligned by column (Filters left, Actions right) to prevent misalignment/overlap.

4. **Email noise reduction** – NotificationService adds:
   - `formatDiffPlain(String)` for readable diffs (also used by the page).
   - `isMeaningfulDiff(String)` to skip initial/empty diffs. A guard is inserted before sending.

> No method names or routes were renamed. Changes are localized and backward-compatible.
