let currentQuizId = null;
const $ = id => document.getElementById(id);
const requestOptions = options => ({credentials: 'include', ...options});

async function loadAuthState() {
  const generateButton = $('generate');
  try {
    const response = await fetch('/api/auth/me', requestOptions({method:'GET'}));
    const user = await response.json();
    if (!response.ok || !user.authenticated) {
      generateButton.disabled = true;
      $('status').textContent = '로그인 후 문제를 생성할 수 있습니다.';
    }
  } catch (error) {
    generateButton.disabled = true;
    $('status').textContent = '로그인 상태를 확인할 수 없습니다.';
  }
}

loadAuthState();

$('generate').addEventListener('click', async () => {
  $('generate').disabled = true;
  $('status').textContent = 'Spring AI가 새 문제를 생성하고 있습니다…';
  $('quiz').classList.add('hidden');
  try {
    const response = await fetch('/api/quizzes', requestOptions({method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({category:$('category').value,difficulty:$('difficulty').value,language:$('language').value})}));
    const quiz = await response.json();
    if (!response.ok) throw new Error(quiz.message || '생성에 실패했습니다.');
    currentQuizId = quiz.quizId;
    $('title').textContent = quiz.title; $('description').textContent = quiz.description;
    $('blankCode').textContent = quiz.blankCode; $('answerCode').textContent = quiz.answerCode;
    $('tests').innerHTML = quiz.testcases.map((t,i)=>`<h3>테스트케이스 ${i+1}</h3><strong>입력</strong><pre>${escapeHtml(t.input)}</pre><strong>출력</strong><pre>${escapeHtml(t.output)}</pre>`).join('');
    $('answers').innerHTML = Array.from({length:quiz.blankCount},(_,i)=>`<label>빈칸 ${i+1}<select class="answer"><option value="">선택하세요</option>${quiz.answerBlocks.map(a=>`<option value="${escapeAttr(a)}">${escapeHtml(a)}</option>`).join('')}</select></label>`).join('');
    $('result').textContent = '모든 빈칸의 답을 선택하세요.'; $('quiz').classList.remove('hidden'); $('status').textContent = '새 문제가 생성되었습니다.';
  } catch (error) { $('status').textContent = error.message; } finally { $('generate').disabled = false; }
});

$('check').addEventListener('click', async () => {
  const answers = [...document.querySelectorAll('.answer')].map(x=>x.value);
  if (answers.some(x=>!x)) { $('result').textContent='모든 빈칸을 선택해주세요.'; return; }
  const response = await fetch(`/api/quizzes/${currentQuizId}/check`,requestOptions({method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({answers})}));
  const result = await response.json();
  $('result').textContent = result.message || '채점에 실패했습니다.';
});

function escapeHtml(value){const d=document.createElement('div');d.textContent=value??'';return d.innerHTML}
function escapeAttr(value){return escapeHtml(value).replaceAll('"','&quot;')}
