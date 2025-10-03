(function() {
  'use strict';

  function compare(a, b, asc) {
    var numA = parseFloat(a.replace(',', '.'));
    var numB = parseFloat(b.replace(',', '.'));
    if (!isNaN(numA) && !isNaN(numB)) {
      return asc ? numA - numB : numB - numA;
    }
    return asc ? a.localeCompare(b) : b.localeCompare(a);
  }

  function sortTable(table, col) {
    var tbody = table.tBodies[0];
    var rows = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
    var currentDir = table.getAttribute('data-sort-dir');
    var currentCol = table.getAttribute('data-sort-col');
    var asc = !(currentCol == col && currentDir === 'asc');

    rows.sort(function(rowA, rowB) {
      var cellA = rowA.children[col].innerText.trim();
      var cellB = rowB.children[col].innerText.trim();
      return compare(cellA, cellB, asc);
    });

    rows.forEach(function(r) { tbody.appendChild(r); });
    table.setAttribute('data-sort-col', col);
    table.setAttribute('data-sort-dir', asc ? 'asc' : 'desc');
  }

  document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('table.sortable').forEach(function(table) {
      table.querySelectorAll('th').forEach(function(th, idx) {
        th.style.cursor = 'pointer';
        th.addEventListener('click', function() { sortTable(table, idx); });
      });
    });
  });
})();