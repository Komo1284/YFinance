let symbols = [];

async function fetchSavedSymbols() {
    const response = await fetch("/get-symbols");
    symbols = await response.json();
    displaySymbols();
}

function addSymbol() {
    const symbol = document.getElementById('symbol').value;
    if (symbol && !symbols[symbol]) {
        fetch(`/add-symbol?symbol=${symbol}`, {method: 'POST'})
            .then(response => {
                if (!response.ok) {  // 응답이 성공적이지 않으면 오류 메시지 처리
                    return response.text().then(message => { throw new Error(message); });
                }
                return response.text();
            })
            .then(() => {
                fetch(`/get-symbol-name?symbol=${symbol}`)
                    .then(response => response.text())
                    .then(name => {
                        symbols[symbol] = name;
                        displaySymbols();
                        document.getElementById('symbol').value = '';
                    });
            })
            .catch(error => {
                alert('存在しないコードです。\nもう一度コードを確認してください。');  // 오류 메시지를 사용자에게 알림창으로 표시
            });
    }
}

function removeSymbol(symbol) {
    // 확인 대화상자를 띄우고, 사용자가 '확인'을 클릭한 경우에만 삭제를 진행합니다.
    if (confirm('本当にリストから削除しますか?')) {
        fetch(`/remove-symbol?symbol=${symbol}`, {method: 'POST'})
            .then(() => {
                delete symbols[symbol];
                displaySymbols();
            })
            .catch(error => console.error('Error removing symbol:', error));
    }
}

function displaySymbols() {
    const symbolList = document.getElementById('symbolList');
    symbolList.innerHTML = '';
    for (const [symbol, name] of Object.entries(symbols)) {
        // 영어 이름 대신 일본어 이름 사용
        fetch(`/get-japanese-name?symbol=${symbol}`)
            .then(response => response.text())
            .then(japaneseName => {
                symbolList.innerHTML += `<li>${japaneseName} (${symbol}) <button class="delete-btn" onclick="removeSymbol('${symbol}')">削除</button></li>`;
            });
    }
}

function resetSymbols() {
    if (confirm("本当に初期状態にリセットしますか?")) {  // 확인문구 추가
        fetch("/reset-symbols", {method: 'POST'})
            .then(() => fetchSavedSymbols())
            .catch(error => console.error('Error resetting symbols:', error));
    }
}

window.onload = function () {
    fetchSavedSymbols();
    setDefaultDates();
};

function setDefaultDates() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('startDate').value = today;
    document.getElementById('endDate').value = today;
}

async function fetchFinancialData() {
    // 로딩 상태 표시
    document.getElementById('loading-container').style.display = 'flex';

    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!startDate || !endDate || Object.keys(symbols).length === 0) {
        alert('すべての入力値を入力してください。');
        document.getElementById('loading-container').style.display = 'none'; // 로딩 상태 숨김
        return;
    }

    const symbolList = Object.keys(symbols).join(',');
    try {
        const response = await fetch(`/financial-data?startDate=${new Date(startDate).getTime() / 1000}&endDate=${new Date(endDate).getTime() / 1000}&symbol=${symbolList}`);
        const data = await response.json();

        if (response.ok) {
            displayData(data);
        } else {
            alert('データの取得に失敗しました。');
        }
    } catch (error) {
        console.error('Error fetching data:', error);
        alert('データの取得に失敗しました。');
    } finally {
        // 데이터 로딩 완료 후 로딩 상태 숨김
        document.getElementById('loading-container').style.display = 'none';
    }
}

function displayData(data) {
    const tableBody = document.getElementById('financialData');
    tableBody.innerHTML = '';

    if (!data || data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="9">データがありません</td></tr>';
        return;
    }

    data.forEach(entry => {
        const symbol = entry.symbol;
        const secUrl = `https://www.sbisec.co.jp/ETGate/?_ControlID=WPLETsiR001Control&_PageID=WPLETsiR001Idtl30&_DataStoreID=DSWPLETsiR001Control&_ActionID=DefaultAID&s_rkbn=2&s_btype=&i_stock_sec=${symbol}&i_dom_flg=1&i_exchange_code=JPN&i_output_type=2&exchange_code=TKY&stock_sec_code_mul=${symbol}&ref_from=1&ref_to=20&wstm4130_sort_id=&wstm4130_sort_kbn=&qr_keyword=1&qr_suggest=1&qr_sort=1`;

        const createCell = (value, isNumeric) => {
            const cellClass = value === '---------' ? 'center-align' : (isNumeric ? 'right-align' : 'center-align');
            return `<td class="${cellClass}">${value}</td>`;
        };

        const row = `
            <tr>
                <td class="center-align">${entry.date}</td>
                <td class="center-align"><a href="${secUrl}" target="_blank">${entry.shortName}</a></td>
                ${createCell(entry.open === 0.0 ? "---------" : entry.open.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price10 === 0.0 ? "---------" : entry.price10.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price11 === 0.0 ? "---------" : entry.price11.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price13 === 0.0 ? "---------" : entry.price13.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price14 === 0.0 ? "---------" : entry.price14.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.close === 0.0 ? "---------" : entry.close.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                <td><input type="text" value=""></td>
            </tr>
        `;
        tableBody.innerHTML += row;
    });
}

function exportToExcel() {
    const table = document.getElementById('financialData');
    const rows = Array.from(table.querySelectorAll('tr'));

    // 헤더와 데이터를 설정
    const header = ["日付", "銘柄", "始値", "10:00", "11:00", "13:00", "14:00", "終値", "備考"];
    const excelData = rows.map(row => {
        const tdCells = Array.from(row.querySelectorAll('td:not(:last-child)')); // 마지막 td를 제외
        const inputCells = Array.from(row.querySelectorAll('input'));
        const tdValues = tdCells.map(td => td.innerText.trim());
        const inputValues = inputCells.map(input => input.value.trim());
        return [...tdValues, ...inputValues];
    });
    excelData.unshift(header);

    // 워크시트 생성
    const ws = XLSX.utils.aoa_to_sheet(excelData);

    // 스타일 설정
    const headerStyle = {
        fill: {fgColor: {rgb: "FFFFE0"}}, // 연한 노란색
        font: {bold: true, color: {rgb: "000000"}, sz: 14}, // 굵은 글씨, 글씨 크기 16
        alignment: {horizontal: "center", vertical: "center"}, // 가운데 정렬
        border: {
            top: {style: "thin", color: {rgb: "000000"}},
            bottom: {style: "thin", color: {rgb: "000000"}}
        }
    };
    const dateStyle = {
        fill: {fgColor: {rgb: "D3D3D3"}}, // 연회색
        font: {bold: true, color: {rgb: "000000"}}, // 굵은 글씨
        alignment: {horizontal: "center", vertical: "center"} // 가운데 정렬
    };
    const nameStyle = {
        fill: {fgColor: {rgb: "ADD8E6"}}, // 연파란색
        font: {bold: true, color: {rgb: "000000"}}, // 굵은 글씨
        alignment: {horizontal: "center", vertical: "center"} // 가운데 정렬
    };
    const centerAlignStyle = {
        alignment: {horizontal: "center", vertical: "center"} // 가운데 정렬
    };
    const rightAlignStyle = {
        alignment: {horizontal: "right", vertical: "center"} // 우측 정렬
    };

    // 열 너비 조정
    ws['!cols'] = header.map((_, i) => ({wpx: i === 1 ? 220 : 120})); // 2열의 너비를 현재의 1.6배로 설정

    // 스타일 적용: 헤더
    for (let i = 0; i < header.length; i++) {
        const cellAddress = {c: i, r: 0}; // 첫 번째 행, 각 열
        const cellRef = XLSX.utils.encode_cell(cellAddress);
        if (!ws[cellRef]) ws[cellRef] = {};
        ws[cellRef].s = headerStyle; // 헤더 스타일 적용
    }

    // 스타일 적용: 날짜와 기업명 열
    for (let r = 1; r < excelData.length; r++) { // 첫 번째 행(헤더)을 제외
        const dateCellAddress = {c: 0, r: r}; // 첫 번째 열, 각 행
        const nameCellAddress = {c: 1, r: r}; // 두 번째 열, 각 행
        const dateCellRef = XLSX.utils.encode_cell(dateCellAddress);
        const nameCellRef = XLSX.utils.encode_cell(nameCellAddress);
        if (!ws[dateCellRef]) ws[dateCellRef] = {};
        if (!ws[nameCellRef]) ws[nameCellRef] = {};
        ws[dateCellRef].s = dateStyle; // 날짜 열 스타일 적용
        ws[nameCellRef].s = nameStyle; // 기업명 열 스타일 적용
    }

    // 스타일 적용: 나머지 셀 (숫자 셀 우측 정렬)
    for (let r = 1; r < excelData.length; r++) { // 첫 번째 행(헤더)을 제외
        for (let c = 2; c < header.length; c++) { // 데이터 열
            const cellAddress = {c: c, r: r};
            const cellRef = XLSX.utils.encode_cell(cellAddress);
            if (!ws[cellRef]) ws[cellRef] = {};

            // 숫자가 들어가는 열(시가, 10:00, 11:00, 13:00, 14:00, 종가)은 우측 정렬, 그 외는 가운데 정렬
            if (c >= 2 && c <= 7) {
                ws[cellRef].s = rightAlignStyle; // 숫자 열은 우측 정렬
            } else {
                ws[cellRef].s = centerAlignStyle; // 나머지는 가운데 정렬
            }
        }
    }

    // 행 높이 조정: 헤더의 행 높이를 1.5배로 설정
    ws['!rows'] = [{hpx: 30}]; // 헤더의 행 높이를 30px로 설정 (기본값보다 1.5배 높음)

    // 워크북 생성 및 시트 추가
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Financial Data');

    // 엑셀 파일 다운로드
    XLSX.writeFile(wb, 'financial_data.xlsx');
}