const employeeDetailModal = document.getElementById('employeeDetailModal');
employeeDetailModal.addEventListener('show.bs.modal', event => {
    const button = event.relatedTarget;
    
    const firstName = button.getAttribute('data-firstname');
    const lastName = button.getAttribute('data-lastname');
    const email = button.getAttribute('data-email');
    const phone = button.getAttribute('data-phone') || 'Non specificato';
    const mobile = button.getAttribute('data-mobile') || 'Non specificato';
    const photo = button.getAttribute('data-photo');

    const modalName = employeeDetailModal.querySelector('#empModalName');
    const modalEmail = employeeDetailModal.querySelector('#empModalEmail');
    const modalPhone = employeeDetailModal.querySelector('#empModalPhone');
    const modalMobile = employeeDetailModal.querySelector('#empModalMobile');
    const modalPhoto = employeeDetailModal.querySelector('#empModalPhoto');

    modalName.textContent = firstName + ' ' + lastName;
    modalEmail.textContent = email;
    modalPhone.textContent = phone;
    modalMobile.textContent = mobile;
    modalPhoto.src = photo;
});