// Small bits of JavaScript used across the site. Pages work without it; it only improves the experience.
(function () {
    'use strict';

    // Bootstrap tooltips (e.g. achievement badges on the home page).
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
        new bootstrap.Tooltip(element);
    });

    document.addEventListener('submit', function (event) {
        var form = event.target;

        // <form data-confirm="Delete this quiz?"> asks before submitting.
        if (form.dataset.confirm && !window.confirm(form.dataset.confirm)) {
            event.preventDefault();
            return;
        }

        // Bootstrap-style validation messages for forms with class "needs-validation".
        if (form.classList.contains('needs-validation')) {
            var password = form.querySelector('#password');
            var confirmation = form.querySelector('#confirmPassword');
            if (password && confirmation) {
                confirmation.setCustomValidity(password.value === confirmation.value ? '' : 'Passwords do not match');
            }
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                form.classList.add('was-validated');
                return;
            }
        }

        // Disable the submit buttons so a double click can't send the form twice. This runs a moment
        // later, because a disabled button's name/value would otherwise be left out of the form data.
        setTimeout(function () {
            form.querySelectorAll('button[type="submit"]').forEach(function (button) {
                button.disabled = true;
            });
        }, 0);
    });

    // Profile previews: a link with data-preview-url opens a small window with a short version of
    // that person's profile. Without JavaScript (or with Ctrl/Cmd-click) it is a normal link.
    var previewWindow = document.getElementById('profilePreview');
    if (previewWindow) {
        var previewBody = previewWindow.querySelector('[data-preview-body]');
        document.addEventListener('click', function (event) {
            var link = event.target.closest('[data-preview-url]');
            if (!link || event.ctrlKey || event.metaKey || event.shiftKey) {
                return;
            }
            event.preventDefault();
            previewBody.innerHTML = '<div class="text-center py-5"><div class="spinner-border" role="status">'
                + '<span class="visually-hidden">Loading...</span></div></div>';
            bootstrap.Modal.getOrCreateInstance(previewWindow).show();
            // The header tells the server to send only the preview, not a whole page.
            fetch(link.dataset.previewUrl, {headers: {'X-Requested-With': 'fetch'}})
                .then(function (response) {
                    // A redirect means the login expired; an error means the user is gone.
                    if (!response.ok || response.redirected) {
                        throw new Error('Preview not available');
                    }
                    return response.text();
                })
                .then(function (html) {
                    previewBody.innerHTML = html;
                })
                .catch(function () {
                    window.location.href = link.href;
                });
        });
    }

    // Quizzes with a time limit: count down, and when time runs out, send what was answered.
    var timer = document.getElementById('quizTimer');
    if (timer) {
        var endsAt = Date.now() + Number(timer.dataset.secondsLeft) * 1000;
        var timerText = timer.querySelector('[data-timer-text]');
        var countdown;
        var showTimeLeft = function () {
            var left = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000));
            timerText.textContent = Math.floor(left / 60) + ':' + String(left % 60).padStart(2, '0');
            if (left <= 30) {
                timer.classList.replace('bg-light', 'bg-danger');
                timer.classList.replace('text-primary', 'text-white');
            }
            if (left === 0) {
                clearInterval(countdown);
                // form.submit() skips the "required" checks, so unanswered questions are simply sent empty.
                var form = document.querySelector('form[data-timer-submit]') || document.getElementById('timeUpForm');
                form.submit();
            }
        };
        countdown = setInterval(showTimeLeft, 250);
        showTimeLeft();
    }

    // Keep the number next to "Messages" up to date without reloading the page.
    var inboxBadge = document.querySelector('[data-inbox-badge]');
    if (inboxBadge) {
        var refreshInboxBadge = function () {
            if (document.hidden) {
                return;
            }
            fetch(inboxBadge.dataset.inboxBadge)
                .then(function (response) {
                    // A redirect means the login expired; then the badge just stays as it is.
                    return response.ok && !response.redirected ? response.json() : null;
                })
                .then(function (data) {
                    if (data) {
                        inboxBadge.textContent = data.count;
                        inboxBadge.hidden = data.count === 0;
                    }
                })
                .catch(function () {
                    // Offline for a moment: try again next time.
                });
        };
        setInterval(refreshInboxBadge, 30000);
        document.addEventListener('visibilitychange', refreshInboxBadge);
    }

    // If the browser shows this page from its back/forward cache, reload it so it is up to date
    // (and never shows a logged-in page after logging out).
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });
})();
