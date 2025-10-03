let companyInfo = null;

async function loadCompanyInfo() {
    if (companyInfo) {
        return companyInfo;
    }
    try {
        const response = await fetch('/api/company-info/primary');
        if (response.ok) {
            companyInfo = await response.json();
        }
    } catch (e) {
        console.error('Errore nel caricamento delle informazioni aziendali', e);
    }
    return companyInfo;
}
/*
function initializeFilterAutoApply() {
    const form = document.getElementById('filterForm');
    if (form) {
        form.querySelectorAll('select, input').forEach(el => {
            el.addEventListener('change', applyFilters);
        });
    }
}
*/
function setupRefuelFormValidation() {
    const form = document.getElementById('refuelForm');
    if (!form) {
        return;
    }

    form.querySelectorAll('.decimal-field').forEach(input => {
        input.addEventListener('input', () => {
            let value = input.value.replace('.', ',').replace(/[^0-9,]/g, '');
            const parts = value.split(',');
            if (parts.length > 2) {
                value = parts[0] + ',' + parts.slice(1).join('');
            }
            if (parts[1]) {
                parts[1] = parts[1].slice(0, 2);
                value = parts[0] + ',' + parts[1];
            }
            input.value = value;
        });
    });

    form.querySelectorAll('.numeric-field').forEach(input => {
        input.addEventListener('input', () => {
            input.value = input.value.replace(/\D/g, '');
        });
    });

    form.addEventListener('submit', event => {
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
			} else {
			            // Converti i valori decimali sostituendo la virgola con il punto
			            form.querySelectorAll('.decimal-field').forEach(input => {
			                if (input.value) {
			                    input.value = input.value.replace(',', '.');
			                }
			            });
        }
        form.classList.add('was-validated');
    });
}

function setupFuelCardAutoSelection() {
    const form = document.getElementById('refuelForm');
    if (!form) {
        return;
    }
    const vehicleSelect = form.querySelector('[name="vehicle.id"]');
    const fuelCardSelect = form.querySelector('[name="fuelCard.id"]');
    if (!vehicleSelect || !fuelCardSelect) {
        return;
    }

    function applyCardSelection() {
        const option = vehicleSelect.options[vehicleSelect.selectedIndex];
        const cardId = option ? option.getAttribute('data-card-id') : '';
        fuelCardSelect.value = cardId ? cardId : '';
    }

    vehicleSelect.addEventListener('change', applyCardSelection);

    if (!fuelCardSelect.value) {
        applyCardSelection();
    }
}

document.addEventListener('DOMContentLoaded', () => {
	setupRefuelFormValidation();
   	loadCompanyInfo();
	setupFuelCardAutoSelection();
    //initializeFilterAutoApply();
});

function applyFilters() {
    const form = document.getElementById('filterForm');
    const data = new FormData(form);
    const params = new URLSearchParams();
    for (const [key, value] of data.entries()) {
        if (value) {
            params.append(key, value);
        }
    }
    const query = params.toString();
    const base = window.location.pathname;
    window.location.href = query ? `${base}?${query}` : base;
}

function resetFilters() {
    const base = window.location.pathname;
    window.location.href = base;
}
/*
function exportExcel(rows) {
    if (!rows || rows.length === 0) {
        alert('Nessun dato da esportare');
        return;
    }

    const headers = Array.from(document.querySelectorAll('#refuelsTable thead th'))
        .slice(0,5)
        .map(function(th){ return th.innerText.trim(); });

    const csv = [headers.join(';')];
    rows.forEach(function(row){
        const cells = Array.from(row.querySelectorAll('td'))
            .slice(0,5)
            .map(function(td){ return td.innerText.trim(); });
        csv.push(cells.join(';'));
    });

    const blob = new Blob([csv.join('\n')], {type:'text/csv;charset=utf-8;'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `rifornimenti_${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}
*/

async function exportPDF() {
    console.log("🔍 Inizio exportPDF...");
	
	await loadCompanyInfo();
	const rows = Array.from(document.querySelectorAll('#refuelsTable tbody tr'));
    
	// Verifica disponibilità jsPDF
    if (!window.jspdf || !window.jspdf.jsPDF) {
        console.error("❌ jsPDF non è disponibile!");
        alert("Errore: Libreria PDF non caricata. Ricarica la pagina e riprova.");
        return;
    }
    console.log("✅ jsPDF disponibile");
    
    // Filtra solo le righe visibili (non nascoste dai filtri)
    const visibleRows = rows.filter(row => !row.classList.contains('d-none'));
    
    if (!visibleRows || visibleRows.length === 0) {
        console.log("⚠️ Nessuna riga da esportare");
        alert('Nessun dato da esportare');
        return;
    }
    console.log(`📊 Trovate ${visibleRows.length} righe da esportare`);
    
    try {
        const headers = ['DATA', 'VEICOLO', 'CARTA', 'LITRI', 'IMPORTO'];
        
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF('l', 'mm', 'a4'); // Orientamento landscape
        
        // Colori professionali (riferimento al layout employments)
        const primaryBlue = [41, 128, 185]; // Blu professionale
        const lightGray = [236, 240, 241]; // Grigio chiaro
        const darkGray = [52, 73, 94]; // Grigio scuro
        const successGreen = [39, 174, 96]; // Verde
        
        // Dimensioni e margini
        const pageWidth = doc.internal.pageSize.getWidth();
        const pageHeight = doc.internal.pageSize.getHeight();
        const margin = 15;
        const contentWidth = pageWidth - (margin * 2);
        
        // Calcola statistiche
        let totalAmount = 0;
        let totalLiters = 0;
        
        visibleRows.forEach(function(row) {
            totalAmount += parseFloat(row.dataset.amount || 0);
            const cells = Array.from(row.querySelectorAll('td'));
            const litersText = cells[3]?.innerText.trim().replace(',', '.').replace(' L', '') || '0';
            totalLiters += parseFloat(litersText) || 0;
        });
        
        const today = new Date().toLocaleDateString('it-IT', {
            day: '2-digit',
            month: '2-digit', 
            year: 'numeric'
        });
        
        // Header del documento (ridotto e ottimizzato)
        function addHeader() {
            // Linea superiore blu ridotta
            doc.setFillColor(...primaryBlue);
            doc.rect(0, 0, pageWidth, 4, 'F');
            
            // Logo/Nome azienda in alto a sinistra (più piccolo)
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...darkGray);
			const companyName = companyInfo?.companyName || 'Veely S.r.l.';
			            doc.text(companyName, margin, 12);
            
            // Titolo documento centrato (più piccolo)
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...primaryBlue);
            doc.text('REPORT RIFORNIMENTI', pageWidth / 2, 12, { align: 'center' });
            
            // Data in alto a destra (più piccola)
            doc.setFontSize(9);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...darkGray);
            doc.text(today, pageWidth - margin, 12, { align: 'right' });
            
            // Linea separatrice più sottile
            doc.setDrawColor(...primaryBlue);
            doc.setLineWidth(0.3);
            doc.line(margin, 16, pageWidth - margin, 16);
        }
        
        // Box statistiche compatto
        function addSummaryBox() {
            const boxY = 22; // Ridotto da 35 a 22
            const boxHeight = 18; // Ridotto da 25 a 18
            
            // Box principale più sottile
            doc.setFillColor(...lightGray);
            doc.rect(margin, boxY, contentWidth, boxHeight, 'F');
            doc.setDrawColor(...primaryBlue);
            doc.setLineWidth(0.2);
            doc.rect(margin, boxY, contentWidth, boxHeight, 'S');
            
            // Contenuto statistiche più compatto
            doc.setFontSize(10); // Ridotto da 11 a 10
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...primaryBlue);
            
            // Riga singola con tutte le statistiche
            const midY = boxY + (boxHeight / 2) + 2;
            
            // Sinistra - Totale rifornimenti
            doc.text('Totale:', margin + 8, midY - 2);
            doc.setTextColor(...darkGray);
            doc.setFont('helvetica', 'normal');
            doc.text(`${visibleRows.length} rifornimenti`, margin + 8, midY + 2);
            
            // Centro - Importo totale
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(...primaryBlue);
            doc.text('Importo Totale:', pageWidth / 2 - 25, midY - 2, { align: 'left' });
            doc.setTextColor(...successGreen);
            doc.text(totalAmount.toLocaleString('it-IT', { style: 'currency', currency: 'EUR' }), 
                    pageWidth / 2 + 25, midY - 2, { align: 'right' });
            
            // Centro-destra - Litri totali
            doc.setTextColor(...primaryBlue);
            doc.text('Litri Totali:', pageWidth / 2 - 25, midY + 2, { align: 'left' });
            doc.setTextColor(...darkGray);
            doc.text(`${totalLiters.toFixed(2)} L`, pageWidth / 2 + 25, midY + 2, { align: 'right' });
            
            // Destra - Company info (se disponibile)
            doc.setFontSize(8);
            doc.setTextColor(...darkGray);
            doc.setFont('helvetica', 'italic');
			if (companyInfo?.vatNumber) {
			                doc.text(`P.IVA: ${companyInfo.vatNumber}`, pageWidth - margin - 5, midY - 2, { align: 'right' });
			            }
			            if (companyInfo?.primaryPhone) {
			                doc.text(`Tel: ${companyInfo.primaryPhone}`, pageWidth - margin - 5, midY + 2, { align: 'right' });
			            }
            
            return boxY + boxHeight + 8; // Ridotto spacing
        }
        
        // Tabella dati (stile employment con colori e allineamenti)
        function addDataTable(startY) {
            // Larghezze colonne ottimizzate per landscape - aumentate per evitare overflow
            const colWidths = [55, 85, 75, 40, 50]; // Totale: 305mm (più spazio per contenuto)
            const colX = [margin];
            for(let i = 1; i < colWidths.length; i++) {
                colX[i] = colX[i-1] + colWidths[i-1];
            }
            
            let currentY = startY;
            
            // Header tabella (stile employment)
            doc.setFillColor(...primaryBlue);
            doc.rect(margin, currentY, contentWidth, 12, 'F');
            
            doc.setFontSize(10);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(255, 255, 255);
            
            headers.forEach(function(header, i) {
                let x = colX[i] + (colWidths[i] / 2);
                // Allineamento specifico per header
                if (i === 3 || i === 4) { // Litri e Importo centrati
                    x = colX[i] + (colWidths[i] / 2);
                }
                doc.text(header, x, currentY + 8, { align: 'center' });
            });
            
            currentY += 12;
            
            // Righe dati
            doc.setFontSize(9);
            let rowIndex = 0;
            
            visibleRows.forEach(function(row) {
                const rowHeight = 12; // Aumentato per evitare sovrapposizioni
                
                // Controllo overflow pagina
                if (currentY + rowHeight > pageHeight - 35) { // Più spazio per footer fisso
                    doc.addPage();
                    addHeader();
                    currentY = 50; // Ridotto spazio dopo header
                    
                    // Ri-aggiungi header tabella
                    doc.setFillColor(...primaryBlue);
                    doc.rect(margin, currentY, contentWidth, 12, 'F');
                    doc.setFontSize(10);
                    doc.setFont('helvetica', 'bold');
                    doc.setTextColor(255, 255, 255);
                    headers.forEach(function(header, i) {
                        const x = colX[i] + (colWidths[i] / 2);
                        doc.text(header, x, currentY + 8, { align: 'center' });
                    });
                    currentY += 12;
                    doc.setFontSize(9);
                }
                
                // Sfondo alternato
                if (rowIndex % 2 === 1) {
                    doc.setFillColor(248, 249, 250);
                    doc.rect(margin, currentY, contentWidth, rowHeight, 'F');
                }
                
                // Bordo riga sottile
                doc.setDrawColor(220, 220, 220);
                doc.setLineWidth(0.1);
                doc.line(margin, currentY + rowHeight, margin + contentWidth, currentY + rowHeight);
                
                // Contenuto celle - estrai dati puliti
                const cells = Array.from(row.querySelectorAll('td')).slice(0, 5).map(function(td, index) {
                    let text = td.innerText.trim();
                    
                    // Pulizia specifica per colonna
                    if (index === 0) { // Data - prendi solo la prima riga (data)
                        const lines = text.split('\n');
                        text = lines[0].trim();
                    } else if (index === 1) { // Veicolo - unisci targa e modello
                        const lines = text.split('\n');
                        if (lines.length > 1) {
                            text = lines[0].trim() + ' - ' + lines[1].trim();
                        }
                    } else if (index === 2) { // Carta - mantieni testo originale
                        // Non modificare il testo della carta, solo pulisci spazi
                        text = text.replace(/\s+/g, ' ').trim();
                    } else if (index === 3) { // Litri - rimuovi completamente " L" e eventuali newline
                        text = text.replace(/\s*L\s*/g, '').replace(/\n/g, '').trim();
                    }
                    
                    return text;
                });
                
                doc.setFont('helvetica', 'normal');
                doc.setTextColor(...darkGray);
                
                cells.forEach(function(cellText, i) {
                    let x = colX[i] + 3; // Padding sinistro
                    let align = 'left';
                    let maxWidth = colWidths[i] - 6; // Larghezza massima con padding
                    
                    // Allineamento e posizionamento specifico per colonna
                    if (i === 0) { // Data - sinistra
                        x = colX[i] + 3;
                        align = 'left';
                    } else if (i === 1) { // Veicolo - sinistra ma limitato
                        x = colX[i] + 3;
                        align = 'left';
                        // Tronca se troppo lungo (più generoso)
                        if (cellText.length > 28) {
                            cellText = cellText.substring(0, 25) + '...';
                        }
                    } else if (i === 2) { // Carta - sinistra con padding, NO troncamento
                        x = colX[i] + 3;
                        align = 'left';
                        // Non troncare la carta, solo ridurre font se necessario
                        if (cellText.length > 20) {
                            doc.setFontSize(8); // Font più piccolo per carte lunghe
                        }
                    } else if (i === 3) { // Litri - destra con L aggiunta
                        x = colX[i] + colWidths[i] - 3;
                        align = 'right';
                        cellText = cellText + ' L'; // Aggiungi L dopo aver pulito
                    } else if (i === 4) { // Importo - destra e verde
                        x = colX[i] + colWidths[i] - 3;
                        align = 'right';
                        doc.setTextColor(...successGreen);
                        doc.setFont('helvetica', 'bold');
                    }
                    
                    // Disegna il testo
                    doc.text(cellText, x, currentY + 8, { 
                        align: align,
                        maxWidth: maxWidth
                    });
                    
                    // Reset formattazione
                    if (i === 4) {
                        doc.setTextColor(...darkGray);
                        doc.setFont('helvetica', 'normal');
                    }
                    if (i === 2 && cellText.length > 20) {
                        doc.setFontSize(9); // Reset font size
                    }
                });
                
                currentY += rowHeight;
                rowIndex++;
            });
            
            // Bordo finale tabella
            doc.setDrawColor(...primaryBlue);
            doc.setLineWidth(0.8);
            doc.line(margin, currentY, margin + contentWidth, currentY);
        }
        
        // Footer fisso in fondo alla pagina
        function addFooter() {
            const footerY = pageHeight - 15; // 2 cm dal bordo inferiore (20mm)
            
            // Linea separatrice footer
            doc.setDrawColor(...lightGray);
            doc.setLineWidth(0.2);
            doc.line(margin, footerY - 5, pageWidth - margin, footerY - 5);
            
            // Informazioni aziendali nel footer
            doc.setFontSize(8);
            doc.setFont('helvetica', 'normal');
            doc.setTextColor(...darkGray);
            
            // Prima riga footer
			const footerName = companyInfo?.companyName || 'Veely S.r.l.';
						            doc.text(`${footerName} - Sistema di Gestione Flotta`, margin, footerY);
            
            // Seconda riga footer con info aziendali
            doc.setFont('helvetica', 'italic');
			const addressParts = [];
			            if (companyInfo?.legalStreet) {
			                let street = companyInfo.legalStreet;
			                if (companyInfo.legalCivicNumber) {
			                    street += `, ${companyInfo.legalCivicNumber}`;
			                }
			                addressParts.push(street);
			            }
			            if (companyInfo?.legalPostalCode || companyInfo?.legalCity) {
			                let city = '';
			                if (companyInfo.legalPostalCode) {
			                    city += `${companyInfo.legalPostalCode} `;
			                }
			                if (companyInfo.legalCity) {
			                    city += companyInfo.legalCity;
			                }
			                if (companyInfo.legalProvince) {
			                    city += ` (${companyInfo.legalProvince})`;
			                }
			                addressParts.push(city.trim());
			            }
			            const address = addressParts.join(' - ');
			            const phonePart = companyInfo?.primaryPhone ? `Tel: ${companyInfo.primaryPhone}` : null;
			            const vatPart = companyInfo?.vatNumber ? `P.IVA: ${companyInfo.vatNumber}` : null;
			            const footerLine = [address, phonePart, vatPart].filter(Boolean).join(' | ');
			            if (footerLine) {
			                doc.text(footerLine, pageWidth / 2, footerY + 4, { align: 'center' });
			            }
        }
        
        // Generazione documento
        addHeader();
        const tableStartY = addSummaryBox();
        addDataTable(tableStartY);
        addFooter();
        
        // Salvataggio
        const filename = `rifornimenti_${new Date().toISOString().split('T')[0]}.pdf`;
        console.log(`💾 Salvando PDF: ${filename}`);
        doc.save(filename);
        console.log("✅ PDF esportato con successo!");
        
    } catch (error) {
        console.error("❌ Errore durante l'esportazione PDF:", error);
        alert("Errore durante la generazione del PDF: " + error.message);
    }
	
	}

	function editRefuel(id) {
	    if (id) {
	        window.location.href = `/fleet/refuels/${id}/edit`;
	    }
	}

	function deleteRefuel(id) {
	    if (id && confirm('Sei sicuro di voler eliminare questo rifornimento?')) {
	        const form = document.getElementById('deleteForm');
	        if (form) {
	            form.action = `/fleet/refuels/${id}/delete`;
	            form.submit();
	        }
	    }
}