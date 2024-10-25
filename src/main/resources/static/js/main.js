let searchSymbols = [];  // 검색에 사용할 심볼 리스트
let memoSymbols = [];  // 메모가 포함된 검색 결과

// 서버에서 저장된 심볼을 불러오고 검색 심볼 리스트를 업데이트
async function fetchSavedSymbols() {
    try {
        const response = await fetch("/get-symbols");
        const savedSymbols = await response.json();

        if (savedSymbols && typeof savedSymbols === 'object') {
            const sortedSymbols = Object.keys(savedSymbols).sort((a, b) => Number(a) - Number(b));
            searchSymbols = sortedSymbols;  // 불러온 심볼로 검색 심볼 리스트를 설정
            displaySymbols(sortedSymbols);
        }
    } catch (error) {
        console.error("Error fetching symbols:", error);
    }
}


// 심볼 추가 시 검색 리스트에 반영
function addSymbol() {
    const symbol = document.getElementById('symbol').value;
    if (symbol && !searchSymbols.includes(symbol)) {
        fetch(`/add-symbol?symbol=${symbol}`, { method: 'POST' })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(message => { throw new Error(message); });
                }
                return response.text();
            })
            .then(() => {
                searchSymbols.push(symbol);  // 검색 심볼 리스트에 추가
                fetchSavedSymbols();
                document.getElementById('symbol').value = '';  // 입력 필드 초기화
            })
            .catch(error => {
                alert('存在しないコードです。\nもう一度コードを確認してください。');
            });
    }
}

// 심볼 삭제 시 검색 리스트에서 삭제
function removeSymbol(symbol) {
    if (confirm('本当にリストから削除しますか?')) {
        fetch(`/remove-symbol?symbol=${symbol}`, { method: 'POST' })
            .then(() => {
                searchSymbols = searchSymbols.filter(s => s !== symbol);  // 검색 심볼 리스트에서 제거
                fetchSavedSymbols();  // 삭제 후 업데이트
            })
            .catch(error => console.error('Error removing symbol:', error));
    }
}


async function displaySymbols(sortedSymbols) {
    const symbolList = document.getElementById('symbolList');
    symbolList.innerHTML = '';

    const symbolPromises = sortedSymbols.map(symbol => {
        return fetch(`/get-japanese-name?symbol=${symbol}`)
            .then(response => response.text())
            .then(japaneseName => {
                return {symbol, japaneseName};
            });
    });

    const symbolData = await Promise.all(symbolPromises);
    symbolData.forEach(({symbol, japaneseName}) => {
        symbolList.innerHTML += `<li>${japaneseName} (${symbol}) <button class="delete-btn" onclick="removeSymbol('${symbol}')">削除</button></li>`;
    });
}

function resetSymbols() {
    if (confirm("初期状態にリセットしても宜しいですか？")) {
        fetch("/reset-symbols", {method: 'POST'})
            .then(() => fetchSavedSymbols())
            .catch(error => console.error('Error resetting symbols:', error));
    }
}

window.onload = function () {
    fetchSavedSymbols();
    setDefaultDates();
    // 마지막 검색 결과 불러오기
    fetch('/get-last-search-results')
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                displayData(data);  // 마지막 검색 결과 출력
            }
        })
        .catch(error => console.error('Error fetching last search results:', error));
};

function setDefaultDates() {
    const today = new Date();
    const threeDaysAgo = new Date();
    threeDaysAgo.setDate(today.getDate() - 2);

    const todayString = today.toISOString().split('T')[0];
    const threeDaysAgoString = threeDaysAgo.toISOString().split('T')[0];

    document.getElementById('startDate').value = threeDaysAgoString; // 3일 전 날짜를 시작 날짜로 설정
    document.getElementById('endDate').value = todayString;          // 오늘 날짜를 종료 날짜로 설정
}

// 금융 데이터를 가져오는 함수
async function fetchFinancialData() {
    document.getElementById('loading-container').style.display = 'flex';

    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!startDate || !endDate || searchSymbols.length === 0) {  // 검색에 필요한 값들이 모두 있는지 확인
        alert('すべての入力値を入力してください。');
        document.getElementById('loading-container').style.display = 'none';
        return;
    }

    const symbolList = searchSymbols.join(',');

    console.log('Requesting data for symbols:', symbolList);

    try {
        const response = await fetch(`/financial-data?startDate=${new Date(startDate).getTime() / 1000}&endDate=${new Date(endDate).getTime() / 1000}&symbol=${symbolList}`);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Error details:', errorText);

            if (response.status === 404) {
                alert('指定されたシンボルに対するデータが見つかりませんでした。');
            } else {
                alert('データの取得に失敗しました。');
            }
            return;
        }

        const data = await response.json();
        displayData(data);  // 데이터를 별도로 처리
    } catch (error) {
        console.error('Error fetching data:', error);
        alert('データの取得に失敗しました。');
    } finally {
        document.getElementById('loading-container').style.display = 'none';
    }
}

// 검색 후 데이터를 서버에 저장
function saveSearchResults(data) {
    fetch('/save-search-results', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    }).catch(error => console.error('Error saving search results:', error));
}

// 검색 결과 및 메모 표시
function displayData(data) {
    const tableBody = document.getElementById('financialData');
    tableBody.innerHTML = '';

    if (!data || data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="9">データがありません</td></tr>';
        return;
    }

    memoSymbols = data;  // 메모가 포함된 검색 결과를 저장

    data.forEach(entry => {
        const symbol = entry.symbol;
        const date = entry.date;
        const isDivider = entry.open === 0.0 && entry.price10 === 0.0 && entry.price11 === 0.0 && entry.price13 === 0.0 && entry.price14 === 0.0 && entry.close === 0.0;
        const secUrl = `https://www.sbisec.co.jp/ETGate/?_ControlID=WPLETsiR001Control&_PageID=WPLETsiR001Idtl30&_DataStoreID=DSWPLETsiR001Control&_ActionID=DefaultAID&s_rkbn=2&s_btype=&i_stock_sec=${symbol}&i_dom_flg=1&i_exchange_code=JPN&i_output_type=2&exchange_code=TKY&stock_sec_code_mul=${symbol}&ref_from=1&ref_to=20`;

        const createCell = (value, isNumeric) => {
            const cellClass = value === '---------' ? 'center-align' : (isNumeric ? 'right-align' : 'center-align');
            return `<td class="${cellClass}">${value}</td>`;
        };

        const companyNameWithSymbol = symbol ? `${entry.shortName} (${symbol})` : entry.shortName;

        let row = `
            <tr>
                <td class="center-align">${date}</td>  <!-- 날짜 -->
                <td class="center-align"><a href="${secUrl}" target="_blank">${companyNameWithSymbol}</a></td>
                ${createCell(entry.open === 0.0 ? "---------" : entry.open.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price10 === 0.0 ? "---------" : entry.price10.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price11 === 0.0 ? "---------" : entry.price11.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price13 === 0.0 ? "---------" : entry.price13.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.price14 === 0.0 ? "---------" : entry.price14.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                ${createCell(entry.close === 0.0 ? "---------" : entry.close.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1}), true)}
                <td class="memo-cell" id="memo-${symbol}-${date}">${entry.memo || ''}</td>
        `;

        // 구분선이 아닌 경우에만 수정 버튼 표시
        if (!isDivider) {
            row += `
                <td class="center-align">
                    <button class="edit-btn" onclick="editMemo('${symbol}', '${date}')">修正</button>  <!-- 날짜를 함께 전달 -->
                </td>
            `;
        } else {
            row += `<td></td>`;  // 구분선인 경우에는 빈 셀 추가
        }

        row += `</tr>`;
        tableBody.innerHTML += row;
    });

    saveSearchResults(data);  // 검색결과와 메모를 서버에 저장
}

function editMemo(symbol, date) {
    const memoCell = document.getElementById(`memo-${symbol}-${date}`);
    const currentMemo = memoCell.innerText;

    memoCell.innerHTML = `
        <input type="text" id="memo-input-${symbol}-${date}" value="${currentMemo}" />
        <button onclick="saveMemo('${symbol}', '${date}')">保存</button>
        <button onclick="cancelEdit('${symbol}', '${date}', '${currentMemo}')">キャンセル</button>
    `;
}

function saveMemo(symbol, date) {
    const newMemo = document.getElementById(`memo-input-${symbol}-${date}`).value;

    // memoSymbols 배열에서 해당 심볼과 날짜를 찾아 메모를 업데이트
    memoSymbols = memoSymbols.map(entry => {
        if (entry.symbol === symbol && entry.date === date) {
            entry.memo = newMemo;  // 메모를 업데이트
        }
        return entry;
    });

    // 메모 저장 후 전체 데이터를 서버에 저장
    fetch('/save-search-results', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(memoSymbols),  // 수정된 전체 데이터를 서버로 전송
    }).then(response => {
        if (response.ok) {
            // 서버 저장이 완료된 후 화면을 업데이트
            document.getElementById(`memo-${symbol}-${date}`).innerHTML = newMemo;  // 수정된 메모를 화면에 표시
        } else {
            alert('メモの保存に失敗しました。');
        }
    }).catch(error => {
        console.error('メモの保存中にエラーが発生しました:', error);
    });
}

function cancelEdit(symbol, date, originalMemo) {
    document.getElementById(`memo-${symbol}-${date}`).innerHTML = originalMemo;
}

// 자동완성 기능
async function fetchMatchingSymbols(query) {
    if (query.length < 2) {
        clearAutocomplete();
        return;
    }

    try {
        const response = await fetch(`/search-symbol?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        displayAutocomplete(data);
    } catch (error) {
        console.error("Error fetching matching symbols:", error);
    }
}

function displayAutocomplete(symbols) {
    clearAutocomplete();
    const autocompleteList = document.getElementById('autocomplete-list');
    symbols.forEach(symbol => {
        const item = document.createElement("div");
        item.innerHTML = `<strong>${symbol.japaneseName}</strong> (${symbol.symbol})`;
        item.addEventListener("click", function() {
            document.getElementById('symbol').value = symbol.symbol;
            clearAutocomplete();
        });
        autocompleteList.appendChild(item);
    });
}

function clearAutocomplete() {
    const autocompleteList = document.getElementById('autocomplete-list');
    autocompleteList.innerHTML = '';
}

function exportToExcel() {
    const table = document.getElementById('financialData');
    const rows = Array.from(table.querySelectorAll('tr'));

    // 테이블의 헤더 및 데이터를 추출
    const header = ["日付", "銘柄", "始値", "10:00", "11:00", "13:00", "14:00", "終値", "備考"];
    const excelData = rows.map(row => {
        const tdCells = Array.from(row.querySelectorAll('td:not(:last-child)')); // 마지막 td 제외
        const inputCells = Array.from(row.querySelectorAll('input')); // 메모 입력 필드 포함
        const tdValues = tdCells.map(td => td.innerText.trim()); // 셀 데이터
        const inputValues = inputCells.map(input => input.value.trim()); // 입력 필드 데이터 (메모)
        return [...tdValues, ...inputValues];
    });

    excelData.unshift(header); // 헤더 추가

    // 엑셀 워크시트 생성
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

    const rightAlignStyle = {
        alignment: {horizontal: "right", vertical: "center"} // 우측 정렬
    };

    const centerAlignStyle = {
        alignment: {horizontal: "center", vertical: "center"} // 가운데 정렬
    };

    // 열 너비 조정
    ws['!cols'] = header.map((_, i) => ({wpx: i === 1 ? 220 : 120})); // 2열의 너비를 조정

    // 스타일 적용: 헤더
    for (let i = 0; i < header.length; i++) {
        const cellAddress = {c: i, r: 0}; // 첫 번째 행, 각 열
        const cellRef = XLSX.utils.encode_cell(cellAddress);
        if (!ws[cellRef]) ws[cellRef] = {};
        ws[cellRef].s = headerStyle; // 헤더 스타일 적용
    }

    // 데이터의 정렬 및 스타일 적용
    for (let r = 1; r < excelData.length; r++) { // 첫 번째 행(헤더)을 제외
        for (let c = 2; c < header.length; c++) { // 데이터 열
            const cellAddress = {c: c, r: r};
            const cellRef = XLSX.utils.encode_cell(cellAddress);
            if (!ws[cellRef]) ws[cellRef] = {};

            // 숫자가 들어가는 열(시가, 10:00, 11:00 등)은 우측 정렬, 나머지는 가운데 정렬
            if (c >= 2 && c <= 7) {
                ws[cellRef].s = rightAlignStyle; // 숫자 열은 우측 정렬
            } else {
                ws[cellRef].s = centerAlignStyle; // 나머지는 가운데 정렬
            }
        }
    }

    // 워크북 생성 및 시트 추가
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Financial Data');

    // 오늘 날짜를 YYYYMMDD 형식으로 가져오기
    const today = new Date();
    const formattedDate = today.toISOString().slice(0, 10).replace(/-/g, ''); // YYYYMMDD 형식으로 변환
    const fileName = `${formattedDate}_FinancialData.xlsx`; // 파일명 생성

    // 엑셀 파일 다운로드
    XLSX.writeFile(wb, fileName);
}