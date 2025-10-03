document.addEventListener('DOMContentLoaded', () => {
    const supplierSelect = document.getElementById('supplier');
    const referentSelect = document.getElementById('supplierReferent');

    if (!supplierSelect || !referentSelect) {
        return;
    }
	
	const getSuppliers = () => window.suppliers || [];

    const resetReferents = () => {
        referentSelect.innerHTML = '<option value="">Seleziona...</option>';
        referentSelect.disabled = true;
    };

    const populateReferents = (supplierId) => {
        resetReferents();
        if (!supplierId) {
            return;
        }
		const supplier = getSuppliers().find(
		            (s) => String(s.id) === String(supplierId)
		        );
        if (!supplier || !supplier.referents) {
            return;
        }
        supplier.referents.forEach((ref) => {
            const option = document.createElement('option');
            option.value = ref.id;
            let label = ref.name || '';
            if (ref.email) {
                label += ` — ${ref.email}`;
            } else if (ref.phone) {
                 label += ` — ${ref.phone}`;
            }
			option.textContent = label;
            referentSelect.appendChild(option);
        });
		
        const selected = referentSelect.dataset.selected;
        if (selected) {
            referentSelect.value = selected;
        }
        referentSelect.disabled = false;
    };

    if (supplierSelect.value) {
        populateReferents(supplierSelect.value);
    }

    supplierSelect.addEventListener('change', (event) => {
        const value = event.target.value;
        referentSelect.dataset.selected = '';
		populateReferents(value);
    });

    referentSelect.addEventListener('change', (event) => {
        referentSelect.dataset.selected = event.target.value;
    });
});