document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('refuelForm');
    const quantityField = document.getElementById('quantity');
    const amountField = document.getElementById('amount');
    const pricePerLiterField = document.getElementById('pricePerLiter');
	const vehicleSelect = form ? form.querySelector('[name="vehicle.id"]') : null;
	const fuelCardSelect = form ? form.querySelector('[name="fuelCard.id"]') : null;
    
    // Fix decimal values on page load (convert dots to commas)
    function fixDecimalValues() {
        if (quantityField && quantityField.value) {
            quantityField.value = quantityField.value.replace('.', ',');
        }
        if (amountField && amountField.value) {
            amountField.value = amountField.value.replace('.', ',');
        }
        // Calculate price on load if values exist
        calculatePricePerLiter();
    }
    
    // Call fix on page load
    fixDecimalValues();
	
	function applyCardSelection() {
	        if (!vehicleSelect || !fuelCardSelect) return;
	        const option = vehicleSelect.options[vehicleSelect.selectedIndex];
	        const cardId = option ? option.getAttribute('data-card-id') : '';
	        fuelCardSelect.value = cardId ? cardId : '';
	    }

	    if (vehicleSelect && fuelCardSelect) {
	        vehicleSelect.addEventListener('change', applyCardSelection);
	        if (!fuelCardSelect.value) {
	            applyCardSelection();
	        }
	    }
    
    // Auto-calculation for price per liter
    function calculatePricePerLiter() {
        if (!quantityField || !amountField || !pricePerLiterField) return;
        
        const quantity = parseFloat(quantityField.value.replace(',', '.'));
        const amount = parseFloat(amountField.value.replace(',', '.'));
        
        if (quantity > 0 && amount > 0) {
            const pricePerLiter = (amount / quantity).toFixed(3);
            pricePerLiterField.value = pricePerLiter.replace('.', ',');
            pricePerLiterField.parentElement.parentElement.classList.add('has-success');
        } else {
            pricePerLiterField.value = '';
            pricePerLiterField.parentElement.parentElement.classList.remove('has-success');
        }
    }
    
    // Real-time validation and calculation
    if (quantityField && amountField) {
        [quantityField, amountField].forEach(field => {
            field.addEventListener('input', function() {
                validateDecimalField(this);
                calculatePricePerLiter();
            });
            
            field.addEventListener('blur', function() {
                formatDecimalField(this);
                calculatePricePerLiter();
            });
        });
    }
    
    // Numeric field validation
    const mileageField = document.getElementById('mileage');
    if (mileageField) {
        mileageField.addEventListener('input', function() {
            validateNumericField(this);
        });
        
        mileageField.addEventListener('blur', function() {
            formatNumericField(this);
        });
    }
    
    // Form validation and submission
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Convert commas to dots before submission for server processing
        if (quantityField && quantityField.value) {
            quantityField.value = quantityField.value.replace(',', '.');
        }
        if (amountField && amountField.value) {
            amountField.value = amountField.value.replace(',', '.');
        }
        
        // Remove all previous validation states
        document.querySelectorAll('.form-field').forEach(field => {
            field.classList.remove('has-error', 'has-success');
        });
        
        let isValid = true;
        
        // Validate required fields
        const requiredFields = form.querySelectorAll('[required]');
        requiredFields.forEach(field => {
            const formField = field.closest('.form-field');
            if (!field.value.trim()) {
                formField.classList.add('has-error');
                isValid = false;
            } else {
                formField.classList.add('has-success');
            }
        });
        
        // Validate decimal fields (now with dots for server)
        const decimalFields = form.querySelectorAll('.decimal-field');
        decimalFields.forEach(field => {
            const formField = field.closest('.form-field');
            if (field.value && !isValidDecimalForServer(field.value)) {
                formField.classList.add('has-error');
                isValid = false;
            }
        });
        
        // Validate numeric fields
        const numericFields = form.querySelectorAll('.numeric-field');
        numericFields.forEach(field => {
            const formField = field.closest('.form-field');
            if (field.value && !isValidNumeric(field.value)) {
                formField.classList.add('has-error');
                isValid = false;
            }
        });
        
        if (isValid) {
            // Add loading state
            const submitBtn = form.querySelector('.btn-submit');
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Salvataggio...';
            
            // Submit form
            form.submit();
        } else {
            // Convert back to commas for display if validation failed
            if (quantityField && quantityField.value) {
                quantityField.value = quantityField.value.replace('.', ',');
            }
            if (amountField && amountField.value) {
                amountField.value = amountField.value.replace('.', ',');
            }
            
            // Scroll to first error
            const firstError = form.querySelector('.has-error');
            if (firstError) {
                firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }
    });
    
    // Utility functions
    function validateDecimalField(field) {
        const formField = field.closest('.form-field');
        if (field.value && !isValidDecimal(field.value)) {
            formField.classList.add('has-error');
            formField.classList.remove('has-success');
        } else if (field.value) {
            formField.classList.remove('has-error');
            formField.classList.add('has-success');
        } else {
            formField.classList.remove('has-error', 'has-success');
        }
    }
    
    function validateNumericField(field) {
        const formField = field.closest('.form-field');
        if (field.value && !isValidNumeric(field.value)) {
            formField.classList.add('has-error');
            formField.classList.remove('has-success');
        } else if (field.value) {
            formField.classList.remove('has-error');
            formField.classList.add('has-success');
        } else {
            formField.classList.remove('has-error', 'has-success');
        }
    }
    
    function formatDecimalField(field) {
        if (field.value) {
            let value = field.value.replace(/[^\d,]/g, '');
            const parts = value.split(',');
            if (parts.length > 2) {
                value = parts[0] + ',' + parts.slice(1).join('');
            }
            if (parts[1] && parts[1].length > 2) {
                value = parts[0] + ',' + parts[1].substring(0, 2);
            }
            field.value = value;
        }
    }
    
    function formatNumericField(field) {
        if (field.value) {
            field.value = field.value.replace(/[^\d]/g, '');
        }
    }
    
    function isValidDecimal(value) {
        return /^\d+(,\d{1,2})?$/.test(value);
    }
    
    function isValidDecimalForServer(value) {
        return /^\d+(\.\d{1,2})?$/.test(value);
    }
    
    function isValidNumeric(value) {
        return /^\d+$/.test(value);
    }
    
    // Auto-focus first field
    const firstField = form.querySelector('input:not([type="hidden"]), select');
    if (firstField) {
        firstField.focus();
    }
    
    // Enhanced keyboard navigation
    const fields = form.querySelectorAll('input, select, button');
    fields.forEach((field, index) => {
        field.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && field.type !== 'submit') {
                e.preventDefault();
                const nextField = fields[index + 1];
                if (nextField) {
                    nextField.focus();
                }
            }
        });
    });
});