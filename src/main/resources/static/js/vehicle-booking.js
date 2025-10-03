(function() {
    document.addEventListener('DOMContentLoaded', function() {
        const app = document.getElementById('bookingPlannerApp');
        if (!app) {
            return;
        }

        const calendarEl = document.getElementById('bookingCalendar');
        const vehicleList = document.getElementById('vehicleList');
        const vehicleItems = () => vehicleList ? Array.from(vehicleList.querySelectorAll('.vehicle-item')) : [];
        const vehicleSearchInput = document.getElementById('vehicleSearchInput');
        const selectedVehicleLabel = document.getElementById('selectedVehicleLabel');
        const availabilityBadge = document.getElementById('availabilityBadge');
        const upcomingContainer = document.getElementById('upcomingBookingsList');
        const plannerAlert = document.getElementById('plannerAlert');
        const refreshBtn = document.getElementById('refreshCalendarBtn');
        const openModalBtn = document.getElementById('openBookingModalBtn');
        const saveBookingBtn = document.getElementById('saveBookingBtn');
        const bookingModalEl = document.getElementById('bookingModal');
        const bookingFormAlert = document.getElementById('bookingFormAlert');
        const bookingDetailModalEl = document.getElementById('bookingDetailModal');
        const deleteBookingBtn = document.getElementById('deleteBookingBtn');

        const bookingIdInput = document.getElementById('bookingId');
        const bookingTitleInput = document.getElementById('bookingTitle');
        const bookingRequesterInput = document.getElementById('bookingRequester');
        const bookingContactInput = document.getElementById('bookingContact');
        const bookingStatusSelect = document.getElementById('bookingStatus');
        const bookingStartInput = document.getElementById('bookingStart');
        const bookingEndInput = document.getElementById('bookingEnd');
        const bookingNotesInput = document.getElementById('bookingNotes');

        const detailTitle = document.getElementById('detailTitle');
        const detailRequester = document.getElementById('detailRequester');
        const detailPeriod = document.getElementById('detailPeriod');
        const detailContact = document.getElementById('detailContact');
        const detailStatus = document.getElementById('detailStatus');
        const detailNotes = document.getElementById('detailNotes');

        const hasVehicles = app.dataset.hasVehicles === 'true';
        let selectedVehicleId = app.dataset.initialVehicleId || null;
        let activeBookings = [];
        let calendar;
        let bookingModal;
        let bookingDetailModal;

        const statusColors = {
            PLANNED: '#2563eb',
            CONFIRMED: '#0f9d58',
            COMPLETED: '#6b7280',
            CANCELLED: '#94a3b8'
        };
        const statusLabels = {
            PLANNED: 'Pianificata',
            CONFIRMED: 'Confermata',
            COMPLETED: 'Completata',
            CANCELLED: 'Annullata'
        };

        const csrfToken = document.querySelector('meta[name="_csrf"]').content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

        function formatDate(date) {
            return new Intl.DateTimeFormat('it-IT', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            }).format(date);
        }

        function toInputValue(date) {
            const pad = (n) => String(n).padStart(2, '0');
            return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
                'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
        }

        function showAlert(message) {
            if (!plannerAlert) return;
            plannerAlert.textContent = message;
            plannerAlert.classList.remove('d-none');
        }

        function hideAlert() {
            if (!plannerAlert) return;
            plannerAlert.classList.add('d-none');
        }

        function clearBookingForm() {
            bookingIdInput.value = '';
            bookingTitleInput.value = '';
            bookingRequesterInput.value = '';
            bookingContactInput.value = '';
            bookingStatusSelect.value = 'PLANNED';
            bookingStartInput.value = '';
            bookingEndInput.value = '';
            bookingNotesInput.value = '';
            bookingFormAlert.classList.add('d-none');
        }

        function updateVehicleSelection(item) {
            vehicleItems().forEach(el => el.classList.remove('active'));
            if (item) {
                item.classList.add('active');
                selectedVehicleId = item.dataset.vehicleId;
                if (selectedVehicleLabel) {
                    selectedVehicleLabel.textContent = item.dataset.vehicleLabel;
                }
            }
        }

        function updateAvailability() {
            if (!availabilityBadge) return;
            if (!activeBookings.length) {
                availabilityBadge.innerHTML = `<span class="badge-label"><i class="bi bi-check-circle"></i>${VEHICLE_BOOKING_I18N.availabilityNow}</span>`;
                return;
            }

            const now = new Date();
            const sorted = [...activeBookings].sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime));
            const current = sorted.find(b => new Date(b.startDateTime) <= now && new Date(b.endDateTime) >= now);
            const next = sorted.find(b => new Date(b.startDateTime) > now);

            if (current) {
                const end = formatDate(new Date(current.endDateTime));
                availabilityBadge.innerHTML = `<span class="badge-label bg-warning-subtle text-warning"><i class="bi bi-exclamation-triangle"></i>${VEHICLE_BOOKING_I18N.busyUntil.replace('{date}', end)}</span>`;
            } else if (next) {
                const nextStart = formatDate(new Date(next.startDateTime));
                availabilityBadge.innerHTML = `<span class="badge-label"><i class="bi bi-clock-history"></i>${VEHICLE_BOOKING_I18N.availableUntil.replace('{date}', nextStart)}</span>`;
            } else {
                availabilityBadge.innerHTML = `<span class="badge-label"><i class="bi bi-check-circle"></i>${VEHICLE_BOOKING_I18N.availabilityNow}</span>`;
            }
        }

        function updateUpcomingList() {
            if (!upcomingContainer) return;
            upcomingContainer.innerHTML = '';
            if (!activeBookings.length) {
                upcomingContainer.innerHTML = `<p class="text-muted mb-0">${VEHICLE_BOOKING_I18N.noBookings}</p>`;
                return;
            }
            const sorted = [...activeBookings]
                .sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime));

            sorted.forEach(booking => {
                const wrapper = document.createElement('div');
                wrapper.className = 'upcoming-booking-card';
                const title = booking.title || booking.requesterName || booking.vehicleLabel;
                wrapper.innerHTML = `
                    <div class="d-flex justify-content-between align-items-start gap-2">
                        <div>
                            <div class="booking-title">${title || 'Prenotazione'}</div>
                            <div class="booking-period">${formatDate(new Date(booking.startDateTime))} → ${formatDate(new Date(booking.endDateTime))}</div>
                            ${booking.requesterName ? `<div class="booking-requester"><i class="bi bi-person"></i> ${booking.requesterName}</div>` : ''}
                        </div>
                        <span class="badge" style="background-color:${statusColors[booking.status] || '#475569'}20;color:${statusColors[booking.status] || '#475569'}">
                            ${statusLabels[booking.status] || booking.status}
                        </span>
                    </div>`;
                upcomingContainer.appendChild(wrapper);
            });
        }

        function refreshCalendar() {
            if (!selectedVehicleId) {
                showAlert(VEHICLE_BOOKING_I18N.noVehicleSelected);
                calendar?.removeAllEvents();
                activeBookings = [];
                updateUpcomingList();
                updateAvailability();
                return;
            }
            hideAlert();
            fetch(`/api/vehicle-bookings?vehicleId=${selectedVehicleId}`)
                .then(res => {
                    if (!res.ok) {
                        throw new Error('Errore nel caricamento delle prenotazioni');
                    }
                    return res.json();
                })
                .then(data => {
                    activeBookings = Array.isArray(data) ? data : [];
                    if (calendar) {
                        calendar.removeAllEvents();
                        const events = activeBookings.map(booking => ({
                            id: booking.id,
                            title: booking.title || booking.requesterName || 'Prenotazione',
                            start: booking.startDateTime,
                            end: booking.endDateTime,
                            backgroundColor: statusColors[booking.status] || '#4f46e5',
                            borderColor: statusColors[booking.status] || '#4f46e5',
                            extendedProps: booking
                        }));
                        calendar.addEventSource(events);
                    }
                    updateUpcomingList();
                    updateAvailability();
                })
                .catch(err => {
                    console.error(err);
                    showAlert('Impossibile recuperare le prenotazioni.');
                });
        }

        function initCalendar() {
            if (!calendarEl) return;
            calendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'timeGridWeek',
                locale: 'it',
                selectable: true,
                nowIndicator: true,
                height: 'auto',
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth,timeGridWeek,timeGridDay'
                },
                businessHours: {
                    daysOfWeek: [1, 2, 3, 4, 5],
                    startTime: '07:00',
                    endTime: '20:00'
                },
                select: function(info) {
                    if (!selectedVehicleId) {
                        showAlert(VEHICLE_BOOKING_I18N.noVehicleSelected);
                        return;
                    }
                    clearBookingForm();
                    bookingStartInput.value = toInputValue(info.start);
                    bookingEndInput.value = toInputValue(info.end);
                    if (!bookingModal) {
                        bookingModal = new bootstrap.Modal(bookingModalEl);
                    }
                    bookingModal.show();
                },
                eventClick: function(info) {
                    const booking = activeBookings.find(b => String(b.id) === info.event.id);
                    if (!booking) {
                        return;
                    }
                    populateDetailModal(booking);
                    if (!bookingDetailModal) {
                        bookingDetailModal = new bootstrap.Modal(bookingDetailModalEl);
                    }
                    bookingDetailModal.show();
                }
            });
            calendar.render();
            refreshCalendar();
        }

        function populateDetailModal(booking) {
            detailTitle.textContent = booking.title || '—';
            detailRequester.textContent = booking.requesterName || '—';
            detailPeriod.textContent = `${formatDate(new Date(booking.startDateTime))} → ${formatDate(new Date(booking.endDateTime))}`;
            detailContact.textContent = booking.requesterContact || '—';
            detailStatus.textContent = statusLabels[booking.status] || booking.status;
            detailStatus.style.backgroundColor = `${statusColors[booking.status] || '#475569'}20`;
            detailStatus.style.color = statusColors[booking.status] || '#475569';
            detailNotes.textContent = booking.notes || '—';
            deleteBookingBtn.dataset.bookingId = booking.id;
        }

        function handleVehicleClick(event) {
            const item = event.currentTarget;
            if (!item || item.classList.contains('active')) {
                return;
            }
            updateVehicleSelection(item);
            refreshCalendar();
        }

        function filterVehicleList() {
            const term = (vehicleSearchInput.value || '').toLowerCase();
            vehicleItems().forEach(item => {
                const label = (item.dataset.vehicleLabel || '').toLowerCase();
                const matches = !term || label.includes(term);
                item.style.display = matches ? '' : 'none';
            });
        }

        function submitBooking() {
            if (!selectedVehicleId) {
                showAlert(VEHICLE_BOOKING_I18N.noVehicleSelected);
                return;
            }
            const payload = {
                vehicleId: Number(selectedVehicleId),
                title: bookingTitleInput.value.trim() || null,
                requesterName: bookingRequesterInput.value.trim() || null,
                requesterContact: bookingContactInput.value.trim() || null,
                startDateTime: bookingStartInput.value,
                endDateTime: bookingEndInput.value,
                notes: bookingNotesInput.value.trim() || null,
                status: bookingStatusSelect.value
            };

            const bookingId = bookingIdInput.value;
            const isUpdate = Boolean(bookingId);
            const url = isUpdate ? `/api/vehicle-bookings/${bookingId}` : '/api/vehicle-bookings';
            const method = isUpdate ? 'PUT' : 'POST';

            fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(payload)
            })
                .then(async res => {
                    if (!res.ok) {
                        const data = await res.json().catch(() => ({}));
                        const messages = Array.isArray(data.errors) ? data.errors.join('\n') : (data.message || 'Errore di validazione');
                        throw new Error(messages);
                    }
                    return res.json();
                })
                .then(() => {
                    if (!bookingModal) {
                        bookingModal = new bootstrap.Modal(bookingModalEl);
                    }
                    bookingModal.hide();
                    refreshCalendar();
                })
                .catch(err => {
                    bookingFormAlert.textContent = err.message;
                    bookingFormAlert.classList.remove('d-none');
                });
        }

        function handleDelete() {
            const bookingId = deleteBookingBtn.dataset.bookingId;
            if (!bookingId) {
                return;
            }
            if (!confirm(VEHICLE_BOOKING_I18N.deleteConfirm)) {
                return;
            }
            fetch(`/api/vehicle-bookings/${bookingId}`, {
                method: 'DELETE',
                headers: {
                    [csrfHeader]: csrfToken
                }
            })
                .then(res => {
                    if (!res.ok) {
                        throw new Error('Impossibile eliminare la prenotazione');
                    }
                    if (!bookingDetailModal) {
                        bookingDetailModal = new bootstrap.Modal(bookingDetailModalEl);
                    }
                    bookingDetailModal.hide();
                    refreshCalendar();
                })
                .catch(err => {
                    alert(err.message);
                });
        }

        if (hasVehicles) {
            vehicleItems().forEach(item => {
                item.addEventListener('click', handleVehicleClick);
            });
            if (selectedVehicleId) {
                const initialItem = vehicleItems().find(item => item.dataset.vehicleId === selectedVehicleId);
                if (initialItem) {
                    updateVehicleSelection(initialItem);
                }
            }
        } else {
            showAlert(VEHICLE_BOOKING_I18N.noVehicleSelected);
        }

        if (vehicleSearchInput) {
            vehicleSearchInput.addEventListener('input', filterVehicleList);
        }

        if (refreshBtn) {
            refreshBtn.addEventListener('click', refreshCalendar);
        }

        if (openModalBtn && bookingModalEl) {
            openModalBtn.addEventListener('click', function() {
                clearBookingForm();
                if (!bookingModal) {
                    bookingModal = new bootstrap.Modal(bookingModalEl);
                }
            });
        }

        if (saveBookingBtn) {
            saveBookingBtn.addEventListener('click', submitBooking);
        }

        if (deleteBookingBtn) {
            deleteBookingBtn.addEventListener('click', handleDelete);
        }

        initCalendar();
    });
})();