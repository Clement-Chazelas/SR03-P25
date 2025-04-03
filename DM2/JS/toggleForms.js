//On récupère le lien vers les éléments qu'on va modifier via leur ID
const toggleLink = document.getElementById('toggle-link');  
const loginForm = document.getElementById('login-form');
const signupForm = document.getElementById('signup-form');
const formTitle = document.getElementById('form-title');

//On ajoute un écouteur d'événement sur le lien pour changer de formulaire (i.e, quand on clique sur le lien "Créer un compte" ou "Retour à la connexion")
toggleLink.addEventListener('click', ()=> {

    //On vérifie quel formulaire est actuellement affiché et on le cache, puis on affiche l'autre formulaire
    if (loginForm.style.display === 'none') {
        loginForm.style.display = 'block';
        signupForm.style.display = 'none';

        //On change le titre du formulaire et le texte du lien en fonction du formulaire affiché
        formTitle.textContent = 'Connexion';
        toggleLink.textContent = 'Créer un nouveau compte';
    } else {
        loginForm.style.display = 'none';
        signupForm.style.display = 'block';

        //On change le titre du formulaire et le texte du lien en fonction du formulaire affiché
        formTitle.textContent = 'Créer un compte';
        toggleLink.textContent = 'Retour à la connexion';
    }
});
